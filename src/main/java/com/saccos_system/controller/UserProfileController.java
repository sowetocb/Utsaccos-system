package com.saccos_system.controller;


import com.saccos_system.dto.*;
import com.saccos_system.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileService userProfileService;
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserProfileDTO> getMyProfile(
            @RequestHeader("Authorization") String token) {
        UserProfileDTO profile = userProfileService.getUserProfile(token);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UpdateProfileDTO request) {
        UserProfileDTO updatedProfile = userProfileService.updateProfile(token, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password (requires current password)")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ChangePasswordDTO request) {
        userProfileService.changePassword(token, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get user dashboard data")
    public ResponseEntity<UserDashboardDTO> getDashboard(
            @RequestHeader("Authorization") String token) {
        UserDashboardDTO dashboard = userProfileService.getDashboardData(token);
        return ResponseEntity.ok(dashboard);
    }
}

