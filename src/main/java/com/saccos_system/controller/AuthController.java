package com.saccos_system.controller;

import com.saccos_system.dto.*;
import com.saccos_system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs for registration, login, password reset")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/initiate")
    @Operation(summary = "Initiate registration by sending OTP to mobile")
    public ResponseEntity<OTPResponseDTO> initiateRegistration(
            @Valid @RequestBody RegisterInitiateDTO request,
            HttpServletRequest httpRequest) {
        OTPResponseDTO response = authService.initiateRegistration(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/complete")
    @Operation(summary = "Complete registration with OTP verification")
    public ResponseEntity<AuthResponseDTO> completeRegistration(
            @Valid @RequestBody RegisterCompleteDTO request,
            HttpServletRequest httpRequest) {
        AuthResponseDTO response = authService.completeRegistration(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username/ID number and password")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody AuthRequestDTO request,
            HttpServletRequest httpRequest) {
        AuthResponseDTO response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/initiate")
    @Operation(summary = "Initiate password reset by sending OTP to mobile")
    public ResponseEntity<OTPResponseDTO> initiatePasswordReset(
            @Valid @RequestBody ForgotPasswordInitiateDTO request,
            HttpServletRequest httpRequest) {
        OTPResponseDTO response = authService.initiatePasswordReset(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/complete")
    @Operation(summary = "Complete password reset with OTP verification")
    public ResponseEntity<AuthResponseDTO> completePasswordReset(
            @Valid @RequestBody ForgotPasswordCompleteDTO request,
            HttpServletRequest httpRequest) {
        AuthResponseDTO response = authService.completePasswordReset(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user by blacklisting token")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        String token = extractToken(authorizationHeader);
        authService.logout(token, httpRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate-token")
    @Operation(summary = "Validate JWT token")
    public ResponseEntity<Boolean> validateToken(
            @RequestHeader("Authorization") String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        boolean isValid = !authService.isTokenBlacklisted(token);
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/admin/unlock/{idNumber}")
    @Operation(summary = "Unlock user account (Admin only)")
    public ResponseEntity<Void> unlockUser(
            @PathVariable String idNumber,
            @RequestParam String adminUsername) {
        authService.unlockUser(idNumber, adminUsername);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/status/{idNumber}")
    @Operation(summary = "Change user status (Admin only)")
    public ResponseEntity<Void> changeUserStatus(
            @PathVariable String idNumber,
            @RequestParam boolean isActive,
            @RequestParam String adminUsername) {
        authService.changeUserStatus(idNumber, isActive, adminUsername);
        return ResponseEntity.ok().build();
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }
}