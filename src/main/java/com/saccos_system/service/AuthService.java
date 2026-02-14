package com.saccos_system.service;

import com.saccos_system.dto.*;
import com.saccos_system.model.*;
import com.saccos_system.repository.*;
import com.saccos_system.util.JwtTokenUtil;
import com.saccos_system.util.OTPUtil;
import com.saccos_system.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final StaffProfileRepository staffProfileRepository;
    private final SystemUserRepository systemUserRepository;
    private final OTPVerificationRepository otpRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final PasswordUtil passwordUtil;
    private final JwtTokenUtil jwtTokenUtil;
    private final OTPUtil otpUtil;
    private final SMSService smsService;
    private final AuthAuditService auditService;
    private final RoleService roleService;  // 🔴 NEW: Add RoleService

    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    @Value("${otp.length:6}")
    private int otpLength;

    @Value("${login.max.attempts:5}")
    private int maxLoginAttempts;

    // ========== REGISTRATION FLOW ==========

    @Transactional
    public OTPResponseDTO initiateRegistration(RegisterInitiateDTO request, HttpServletRequest httpRequest) {
        // Check if ID number exists in StaffProfile
        Optional<StaffProfile> profileOpt = staffProfileRepository.findByIdNumber(request.getIdNumber());
        if (profileOpt.isEmpty()) {
            auditService.logAuthEvent(null, request.getIdNumber(), "REGISTER",
                    "FAILED", "ID Number not found", httpRequest);
            throw new RuntimeException("ID Number not found in our records");
        }

        StaffProfile profile = profileOpt.get();

        // Check if user already registered
        if (systemUserRepository.existsByProfile_IdNumber(request.getIdNumber())) {
            auditService.logAuthEvent(null, request.getIdNumber(), "REGISTER",
                    "FAILED", "User already registered", httpRequest);
            throw new RuntimeException("User with this ID Number is already registered");
        }

        // Check phone number
        if (profile.getPhone() == null || profile.getPhone().trim().isEmpty()) {
            throw new RuntimeException("Phone number not found in profile. Please contact administrator.");
        }

        // Check OTP rate limiting
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        int otpCount = otpRepository.countByIdNumberAndOtpTypeAndCreatedDateAfter(
                request.getIdNumber(), "REGISTRATION", oneHourAgo);

        if (otpCount >= 3) {
            throw new RuntimeException("Too many OTP requests. Please try again later.");
        }

        // Generate and save OTP
        String otpCode = otpUtil.generateOTP(otpLength);
        LocalDateTime expiryDate = otpUtil.calculateExpiryTime(otpExpiryMinutes);

        OTPVerification otp = new OTPVerification();
        otp.setIdNumber(request.getIdNumber());
        otp.setOtpCode(otpCode);
        otp.setOtpType("REGISTRATION");
        otp.setPhoneNumber(profile.getPhone());
        otp.setExpiryDate(expiryDate);
        otpRepository.save(otp);

        // Send OTP via SMS
        smsService.sendOTP(profile.getPhone(), otpCode);

        auditService.logAuthEvent(null, request.getIdNumber(), "OTP_SENT",
                "SUCCESS", "OTP sent for registration", httpRequest);

        return new OTPResponseDTO(
                "OTP sent to registered mobile number",
                profile.getPhone(),
                otpExpiryMinutes
        );
    }

    @Transactional
    public AuthResponseDTO completeRegistration(RegisterCompleteDTO request, HttpServletRequest httpRequest) {
        // Validate OTP
        Optional<OTPVerification> otpOpt = otpRepository.findByIdNumberAndOtpCodeAndOtpTypeAndIsUsedFalse(
                request.getIdNumber(), request.getOtpCode(), "REGISTRATION");

        if (otpOpt.isEmpty()) {
            auditService.logAuthEvent(null, request.getIdNumber(), "OTP_VERIFIED",
                    "FAILED", "Invalid OTP", httpRequest);
            throw new RuntimeException("Invalid or expired OTP");
        }

        OTPVerification otp = otpOpt.get();

        // Check if OTP expired
        if (otpUtil.isOTPExpired(otp.getExpiryDate())) {
            auditService.logAuthEvent(null, request.getIdNumber(), "OTP_VERIFIED",
                    "FAILED", "Expired OTP", httpRequest);
            throw new RuntimeException("OTP has expired");
        }

        // Check passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Check username availability
        if (systemUserRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        // Get profile
        StaffProfile profile = staffProfileRepository.findByIdNumber(request.getIdNumber())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // Create user
        SystemUser user = new SystemUser();
        user.setProfile(profile);
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordUtil.encodePassword(request.getPassword()));
        user.setEmail(profile.getEmail());
        user.setPhone(profile.getPhone());
        user.setCreatedBy("SELF_REGISTERED");

        SystemUser savedUser = systemUserRepository.save(user);

        // Assign default MEMBER role to new users
        try {
            roleService.assignRoleToUser(savedUser.getUserId(), "MEMBER", "SYSTEM");
            log.info("Default MEMBER role assigned to new user: {}", savedUser.getUsername());
        } catch (Exception e) {
            log.error("Failed to assign default role to user: {}", e.getMessage());
            // Don't throw exception - user can still login but may have limited access
        }

        // Mark OTP as used
        otp.setIsUsed(true);
        otp.setUsedDate(LocalDateTime.now());
        otpRepository.save(otp);

        //  Get user roles for token generation
        List<String> roles = roleService.getUserRoles(savedUser.getUserId());
        log.info("User {} has roles: {}", savedUser.getUsername(), roles);

        // Generate token with roles
        String token = jwtTokenUtil.generateToken(savedUser, roles);

        auditService.logAuthEvent(savedUser.getUserId(), request.getIdNumber(), "REGISTER",
                "SUCCESS", "Registration completed", httpRequest);

        // Return response with roles
        return new AuthResponseDTO(
                token,
                savedUser.getUserId(),
                savedUser.getUsername(),
                profile.getFirstName() + " " + profile.getLastName(),
                profile.getMemberNumber(),
                roles,  //  Pass all roles
                jwtTokenUtil.getExpirationDateFromToken(token).getTime() - System.currentTimeMillis()
        );
    }

    // ========== LOGIN ==========

    @Transactional
    public AuthResponseDTO login(AuthRequestDTO request, HttpServletRequest httpRequest) {
        String usernameOrIdNumber = request.getUsernameOrIdNumber();
        String password = request.getPassword();

        // Find user by username or ID number
        Optional<SystemUser> userOpt = findUserByIdentifier(usernameOrIdNumber);

        if (userOpt.isEmpty()) {
            auditService.logAuthEvent(null, usernameOrIdNumber, "LOGIN",
                    "FAILED", "User not found", httpRequest);
            throw new RuntimeException("Invalid credentials");
        }

        SystemUser user = userOpt.get();

        // Check if account is active
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            auditService.logAuthEvent(user.getUserId(), user.getProfile().getIdNumber(), "LOGIN",
                    "FAILED", "Account inactive", httpRequest);
            throw new RuntimeException("Account is inactive");
        }

        // Check if account is locked
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            auditService.logAuthEvent(user.getUserId(), user.getProfile().getIdNumber(), "LOGIN",
                    "FAILED", "Account locked", httpRequest);
            throw new RuntimeException("Account is locked. Please contact administrator.");
        }

        // Verify password
        if (!passwordUtil.matches(password, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            // Lock account after max attempts
            if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
                user.setIsLocked(true);
            }

            systemUserRepository.save(user);

            auditService.logAuthEvent(user.getUserId(), user.getProfile().getIdNumber(), "LOGIN",
                    "FAILED", "Invalid password", httpRequest);
            throw new RuntimeException("Invalid credentials");
        }

        // Successful login - reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLastLoginDate(LocalDateTime.now());
        systemUserRepository.save(user);

        // Get user roles
        List<String> roles = roleService.getUserRoles(user.getUserId());
        log.info("User {} logged in with roles: {}", user.getUsername(), roles);

        // Generate token with roles
        String token = jwtTokenUtil.generateToken(user, roles);

        auditService.logAuthEvent(user.getUserId(), user.getProfile().getIdNumber(), "LOGIN",
                "SUCCESS", "Login successful", httpRequest);

        // Return response with roles
        return new AuthResponseDTO(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getProfile().getFirstName() + " " + user.getProfile().getLastName(),
                user.getProfile().getMemberNumber(),
                roles,  //  Pass all roles
                jwtTokenUtil.getExpirationDateFromToken(token).getTime() - System.currentTimeMillis()
        );
    }

    // ========== FORGOT PASSWORD FLOW ==========

    @Transactional
    public OTPResponseDTO initiatePasswordReset(ForgotPasswordInitiateDTO request, HttpServletRequest httpRequest) {
        // Check if ID number exists in SystemUser
        Optional<SystemUser> userOpt = systemUserRepository.findByProfile_IdNumber(request.getIdNumber());
        if (userOpt.isEmpty()) {
            auditService.logAuthEvent(null, request.getIdNumber(), "PASSWORD_RESET",
                    "FAILED", "User not found", httpRequest);
            throw new RuntimeException("ID Number not found in registered users");
        }

        SystemUser user = userOpt.get();

        // Check OTP rate limiting
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        int otpCount = otpRepository.countByIdNumberAndOtpTypeAndCreatedDateAfter(
                request.getIdNumber(), "PASSWORD_RESET", oneHourAgo);

        if (otpCount >= 3) {
            throw new RuntimeException("Too many OTP requests. Please try again later.");
        }

        // Generate and save OTP
        String otpCode = otpUtil.generateOTP(otpLength);
        LocalDateTime expiryDate = otpUtil.calculateExpiryTime(otpExpiryMinutes);

        OTPVerification otp = new OTPVerification();
        otp.setIdNumber(request.getIdNumber());
        otp.setOtpCode(otpCode);
        otp.setOtpType("PASSWORD_RESET");
        otp.setPhoneNumber(user.getPhone());
        otp.setExpiryDate(expiryDate);
        otpRepository.save(otp);

        // Send OTP via SMS
        smsService.sendOTP(user.getPhone(), otpCode);

        auditService.logAuthEvent(user.getUserId(), request.getIdNumber(), "OTP_SENT",
                "SUCCESS", "OTP sent for password reset", httpRequest);

        return new OTPResponseDTO(
                "OTP sent to registered mobile number",
                user.getPhone(),
                otpExpiryMinutes
        );
    }

    @Transactional
    public AuthResponseDTO completePasswordReset(ForgotPasswordCompleteDTO request, HttpServletRequest httpRequest) {
        // Validate OTP
        Optional<OTPVerification> otpOpt = otpRepository.findByIdNumberAndOtpCodeAndOtpTypeAndIsUsedFalse(
                request.getIdNumber(), request.getOtpCode(), "PASSWORD_RESET");

        if (otpOpt.isEmpty()) {
            auditService.logAuthEvent(null, request.getIdNumber(), "OTP_VERIFIED",
                    "FAILED", "Invalid OTP", httpRequest);
            throw new RuntimeException("Invalid or expired OTP");
        }

        OTPVerification otp = otpOpt.get();

        // Check if OTP expired
        if (otpUtil.isOTPExpired(otp.getExpiryDate())) {
            auditService.logAuthEvent(null, request.getIdNumber(), "OTP_VERIFIED",
                    "FAILED", "Expired OTP", httpRequest);
            throw new RuntimeException("OTP has expired");
        }

        // Check passwords match
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        // Get user
        SystemUser user = systemUserRepository.findByProfile_IdNumber(request.getIdNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password
        user.setPasswordHash(passwordUtil.encodePassword(request.getNewPassword()));
        user.setPasswordChangedDate(LocalDateTime.now());
        user.setFailedLoginAttempts(0); // Reset failed attempts
        user.setIsLocked(false); // Unlock if locked

        SystemUser savedUser = systemUserRepository.save(user);

        // Mark OTP as used
        otp.setIsUsed(true);
        otp.setUsedDate(LocalDateTime.now());
        otpRepository.save(otp);

        //  Get user roles
        List<String> roles = roleService.getUserRoles(savedUser.getUserId());

        // Generate new token with roles
        String token = jwtTokenUtil.generateToken(savedUser, roles);

        auditService.logAuthEvent(savedUser.getUserId(), request.getIdNumber(), "PASSWORD_RESET",
                "SUCCESS", "Password reset completed", httpRequest);

        // Return response with roles
        return new AuthResponseDTO(
                token,
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getProfile().getFirstName() + " " + savedUser.getProfile().getLastName(),
                savedUser.getProfile().getMemberNumber(),
                roles, //Pass all roles
                jwtTokenUtil.getExpirationDateFromToken(token).getTime() - System.currentTimeMillis()
        );
    }

    // ========== LOGOUT ==========

    @Transactional
    public void logout(String token, HttpServletRequest httpRequest) {
        if (blacklistedTokenRepository.existsByToken(token)) {
            return; // Already blacklisted
        }

        String username = jwtTokenUtil.getUsernameFromToken(token);
        Date expiryDate = jwtTokenUtil.getExpirationDateFromToken(token);

        // Add token to blacklist
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setExpiryDate(expiryDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        blacklistedToken.setReason("LOGOUT");
        blacklistedTokenRepository.save(blacklistedToken);

        // Find user for audit log
        Optional<SystemUser> userOpt = systemUserRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            SystemUser user = userOpt.get();
            auditService.logAuthEvent(user.getUserId(), user.getProfile().getIdNumber(), "LOGOUT",
                    "SUCCESS", "User logged out", httpRequest);
        }
    }

    // ========== HELPER METHODS ==========

    private Optional<SystemUser> findUserByIdentifier(String identifier) {
        // Try username first
        Optional<SystemUser> userOpt = systemUserRepository.findByUsername(identifier);

        // If not found by username, try ID number
        if (userOpt.isEmpty()) {
            userOpt = systemUserRepository.findByProfile_IdNumber(identifier);
        }

        return userOpt;
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        blacklistedTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());

        // Also cleanup expired OTPs
        LocalDateTime now = LocalDateTime.now();
        List<OTPVerification> expiredOtps = otpRepository.findAll().stream()
                .filter(otp -> now.isAfter(otp.getExpiryDate()))
                .toList();

        otpRepository.deleteAll(expiredOtps);
    }

    // ========== ADMIN METHODS ==========

    @Transactional
    public void unlockUser(String idNumber, String adminUsername) {
        Optional<SystemUser> userOpt = systemUserRepository.findByProfile_IdNumber(idNumber);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        SystemUser user = userOpt.get();
        user.setIsLocked(false);
        user.setFailedLoginAttempts(0);
        user.setModifiedBy(adminUsername);
        systemUserRepository.save(user);
    }

    @Transactional
    public void changeUserStatus(String idNumber, boolean isActive, String adminUsername) {
        Optional<SystemUser> userOpt = systemUserRepository.findByProfile_IdNumber(idNumber);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        SystemUser user = userOpt.get();
        user.setIsActive(isActive);
        user.setModifiedBy(adminUsername);
        systemUserRepository.save(user);
    }

    //  Method to check if user has specific role
    public boolean userHasRole(Long userId, String roleName) {
        return roleService.userHasRole(userId, roleName);
    }

    //  Method to get current user's roles from token
    public List<String> getCurrentUserRoles(String token) {
        return jwtTokenUtil.getRolesFromToken(token);
    }
}