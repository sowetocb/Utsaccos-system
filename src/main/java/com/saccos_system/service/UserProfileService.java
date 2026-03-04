
package com.saccos_system.service;

import com.saccos_system.dto.AuthDTO.ChangePasswordDTO;
import com.saccos_system.dto.NotificationDTO.NotificationDTO;
import com.saccos_system.dto.TransactionDTO.TransactionDTO;
import com.saccos_system.dto.UserDTO.UpdateProfileDTO;
import com.saccos_system.dto.UserDTO.UserDashboardDTO;
import com.saccos_system.dto.UserDTO.UserProfileDTO;
import com.saccos_system.model.*;
import com.saccos_system.repository.*;
import com.saccos_system.util.JwtTokenUtil;
import com.saccos_system.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileService {

    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordUtil passwordUtil;
    private final SystemUserRepository userRepository;
    private final StaffProfileRepository profileRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final TransactionRecordRepository transactionRepository;
    private final AuthAuditService auditService;

    public UserProfileDTO getUserProfile(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));

        SystemUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileDTO dto = new UserProfileDTO();

        // User info
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setIsActive(user.getIsActive());
        dto.setLastLoginDate(user.getLastLoginDate());
        dto.setPasswordChangedDate(user.getPasswordChangedDate());

        // Profile info
        StaffProfile profile = user.getProfile();
        if (profile != null) {
            dto.setProfileId(profile.getProfileId());
            dto.setMemberNumber(profile.getMemberNumber());
            dto.setFirstName(profile.getFirstName());
            dto.setLastName(profile.getLastName());
            dto.setIdNumber(profile.getIdNumber());
            dto.setDateOfBirth(profile.getDateOfBirth());
            dto.setJoinDate(profile.getJoinDate());

            if (profile.getStatus() != null) {
                dto.setStatusName(profile.getStatus().getStatusName());
            }
        }

        // Savings info
        Optional<SavingsAccount> savingsOpt = savingsAccountRepository.findByProfile_ProfileId(profile.getProfileId());
        if (savingsOpt.isPresent()) {
            SavingsAccount savings = savingsOpt.get();
            dto.setAccountNumber(savings.getAccountNumber());
            dto.setSavingsBalance(savings.getBalance().doubleValue());
        }

        // Loan info
        List<Loan> activeLoans = loanRepository.findByProfile_ProfileIdAndStatus_StatusCode(
                profile.getProfileId(), "LOAN_ACTIVE");
        if (!activeLoans.isEmpty()) {
            BigDecimal totalLoanBalance = activeLoans.stream()
                    .map(Loan::getRemainingBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setLoanBalance(totalLoanBalance.doubleValue());
        }

        return dto;
    }

    public UserProfileDTO updateProfile(String token, UpdateProfileDTO request) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));

        SystemUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email already exists
            Optional<SystemUser> existingUser = userRepository.findByEmail(request.getEmail());
            if (existingUser.isPresent() && !existingUser.get().getUserId().equals(userId)) {
                throw new RuntimeException("Email already in use by another account");
            }

            user.setEmail(request.getEmail());
        }

        // Update phone if provided and different
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            user.setPhone(request.getPhone());
        }
        user.setUsername(request.getUsername());
        user.setModifiedDate(LocalDateTime.now());
        user.setModifiedBy(user.getUsername());

        SystemUser updatedUser = userRepository.save(user);

        return getUserProfile(token);
    }

    public void changePassword(String token, ChangePasswordDTO request) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));

        SystemUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordUtil.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Verify new passwords match
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new RuntimeException("New passwords do not match");
        }

        // Verify new password is different from current
        if (passwordUtil.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordUtil.encodePassword(request.getNewPassword()));
        user.setPasswordChangedDate(LocalDateTime.now());
        user.setFailedLoginAttempts(0); // Reset failed attempts
        user.setIsLocked(false); // Unlock if locked

        userRepository.save(user);

        // Log password change event
        log.info("Password changed for user: {}", user.getUsername());
    }

    public UserDashboardDTO getDashboardData(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));

        SystemUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        StaffProfile profile = user.getProfile();
        if (profile == null) {
            throw new RuntimeException("Profile not found");
        }

        UserDashboardDTO dashboard = new UserDashboardDTO();

        // Basic info
        dashboard.setMemberName(profile.getFirstName() + " " + profile.getLastName());
        dashboard.setMemberNumber(profile.getMemberNumber());
        dashboard.setMemberSince(formatDate(profile.getJoinDate()));

        // Savings data
        Optional<SavingsAccount> savingsOpt = savingsAccountRepository.findByProfile_ProfileId(profile.getProfileId());
        if (savingsOpt.isPresent()) {
            SavingsAccount savings = savingsOpt.get();
            dashboard.setTotalSavings(savings.getBalance());

            // Calculate last month savings (simplified - in reality would query transactions)
            dashboard.setLastMonthSavings(savings.getBalance().multiply(BigDecimal.valueOf(0.05))); // 5% as example
            dashboard.setInterestEarned(savings.getBalance().multiply(
                    savings.getInterestRate().divide(BigDecimal.valueOf(100))));

            if (savings.getStatus() != null) {
                dashboard.setSavingsAccountStatus(savings.getStatus().getStatusName());
            }
        }

        // Loans data
        List<Loan> activeLoans = loanRepository.findByProfile_ProfileIdAndStatus_StatusCode(
                profile.getProfileId(), "LOAN_ACTIVE");
        dashboard.setActiveLoans(activeLoans.size());

        BigDecimal totalLoanBalance = activeLoans.stream()
                .map(Loan::getRemainingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalLoanBalance(totalLoanBalance);

        // Calculate next payment (simplified)
        if (!activeLoans.isEmpty()) {
            Loan nextLoan = activeLoans.get(0); // Get first active loan
            dashboard.setNextPaymentAmount(nextLoan.getMonthlyPayment());
            dashboard.setNextPaymentDate(formatDate(LocalDate.now().plusDays(30))); // Next month as example
        }

        // Recent transactions (last 5)
        List<TransactionRecord> recentTransactions = transactionRepository
                .findTop5BySavingsAccount_Profile_ProfileIdOrderByTransactionDateDesc(profile.getProfileId());

        List<TransactionDTO> transactionDTOs = new ArrayList<>();
        for (TransactionRecord tr : recentTransactions) {
            TransactionDTO tdto = new TransactionDTO();
            tdto.setDate(formatDateTime(tr.getTransactionDate()));
            tdto.setDescription(tr.getDescription());
            tdto.setAmount(tr.getAmount());
            tdto.setType(tr.getTransactionType());
            tdto.setBalanceAfter(tr.getBalanceAfter());
            transactionDTOs.add(tdto);
        }
        dashboard.setRecentTransactions(transactionDTOs);

        // Notifications (mock data for now)
        List<NotificationDTO> notifications = new ArrayList<>();
        notifications.add(createNotification("Welcome!", "Welcome to SACCO System", LocalDateTime.now().minusDays(1), false));
        notifications.add(createNotification("Monthly Statement", "Your January statement is ready", LocalDateTime.now().minusDays(3), true));
        dashboard.setUnreadNotifications(notifications);

        // Available actions
        List<String> actions = new ArrayList<>();
        actions.add("Make Deposit");
        actions.add("Apply for Loan");
        actions.add("Update Profile");
        actions.add("View Statements");
        dashboard.setAvailableActions(actions);

        return dashboard;
    }

    // Helper methods
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return date.format(formatter);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        return dateTime.format(formatter);
    }

    private NotificationDTO createNotification(String title, String message, LocalDateTime date, boolean isRead) {
        NotificationDTO notification = new NotificationDTO();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCreatedDate(date);  // Changed from setDate to setCreatedDate
        notification.setIsRead(isRead);
        return notification;
    }
}

