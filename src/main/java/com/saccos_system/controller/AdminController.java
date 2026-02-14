package com.saccos_system.controller;

import com.saccos_system.dto.*;
import com.saccos_system.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Panel", description = "Administrative operations")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    // User Management
    @GetMapping("/users")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<List<AdminUserDTO>> getAllUsers(
            @RequestHeader("Authorization") String token) {
        log.info("Admin requested all users list");
        List<AdminUserDTO> users = adminService.getAllUsers(token);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Update user status (Admin only)")
    public ResponseEntity<Void> updateUserStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            @RequestParam Boolean isActive) {
        log.info("Admin updating user {} status to {}", userId, isActive);
        adminService.updateUserStatus(token, userId, isActive);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/unlock")
    @Operation(summary = "Unlock user account (Admin only)")
    public ResponseEntity<Void> unlockUserAccount(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        log.info("Admin unlocking user {}", userId);
        adminService.unlockUserAccount(token, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}/roles")
    @Operation(summary = "Get user roles (Admin only)")
    public ResponseEntity<List<String>> getUserRoles(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        log.info("Admin fetching roles for user {}", userId);
        List<String> roles = adminService.getUserRoles(token, userId);
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/users/{userId}/roles/{roleName}")
    @Operation(summary = "Assign role to user (Admin only)")
    public ResponseEntity<Void> assignRole(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            @PathVariable String roleName) {
        log.info("Admin assigning role {} to user {}", roleName, userId);
        adminService.assignRole(token, userId, roleName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @Operation(summary = "Remove role from user (Admin only)")
    public ResponseEntity<Void> removeRole(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            @PathVariable String roleName) {
        log.info("Admin removing role {} from user {}", roleName, userId);
        adminService.removeRole(token, userId, roleName);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{userId}/activity")
    @Operation(summary = "Get user activity log (Admin only)")
    public ResponseEntity<List<ActivityLogDTO>> getUserActivity(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        log.info("Admin fetching activity for user {}", userId);
        List<ActivityLogDTO> activity = adminService.getUserActivity(token, userId);
        return ResponseEntity.ok(activity);
    }

    // Loan Management
    @GetMapping("/loans/pending")
    @Operation(summary = "Get pending loan applications (Loan Officer/Admin)")
    public ResponseEntity<List<AdminLoanApplicationDTO>> getPendingLoans(
            @RequestHeader("Authorization") String token) {
        log.info("Fetching pending loan applications");
        List<AdminLoanApplicationDTO> applications = adminService.getPendingLoanApplications(token);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/loans/{applicationId}/approve")
    @Operation(summary = "Approve loan application (Loan Officer/Admin)")
    public ResponseEntity<Void> approveLoan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long applicationId,
            @RequestBody LoanApprovalDTO approval) {
        log.info("Approving loan application {}", applicationId);
        adminService.approveLoanApplication(token, applicationId, approval);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/loans/{applicationId}/reject")
    @Operation(summary = "Reject loan application (Loan Officer/Admin)")
    public ResponseEntity<Void> rejectLoan(
            @RequestHeader("Authorization") String token,
            @PathVariable Long applicationId,
            @RequestBody LoanRejectionDTO rejection) {
        log.info("Rejecting loan application {}", applicationId);
        adminService.rejectLoanApplication(token, applicationId, rejection);
        return ResponseEntity.ok().build();
    }

    // Reports
    @GetMapping("/reports/savings")
    @Operation(summary = "Get savings report (Accountant/Admin)")
    public ResponseEntity<SavingsReportDTO> getSavingsReport(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Generating savings report from {} to {}", startDate, endDate);
        SavingsReportDTO report = adminService.getSavingsReport(token, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/loans")
    @Operation(summary = "Get loans report (Accountant/Admin)")
    public ResponseEntity<LoansReportDTO> getLoansReport(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Generating loans report from {} to {}", startDate, endDate);
        LoansReportDTO report = adminService.getLoansReport(token, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/transactions")
    @Operation(summary = "Get transactions report (Accountant/Admin)")
    public ResponseEntity<TransactionsReportDTO> getTransactionsReport(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Generating transactions report from {} to {}", startDate, endDate);
        TransactionsReportDTO report = adminService.getTransactionsReport(token, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    // System Settings
    @GetMapping("/settings")
    @Operation(summary = "Get system settings (Admin only)")
    public ResponseEntity<SystemSettingsDTO> getSystemSettings(
            @RequestHeader("Authorization") String token) {
        log.info("Fetching system settings");
        SystemSettingsDTO settings = adminService.getSystemSettings(token);
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    @Operation(summary = "Update system settings (Admin only)")
    public ResponseEntity<Void> updateSystemSettings(
            @RequestHeader("Authorization") String token,
            @RequestBody SystemSettingsDTO settings) {
        log.info("Updating system settings");
        adminService.updateSystemSettings(token, settings);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/ping")
    public String ping() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin ping - Auth: {}", auth);
        log.info("Admin ping - Authorities: {}", auth.getAuthorities());
        return "Admin ping successful";
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard data (Admin only)")
    public ResponseEntity<AdminDashboardDTO> getAdminDashboard(
            @RequestHeader("Authorization") String token) {



        AdminDashboardDTO dashboard = adminService.getAdminDashboard(token);
        return ResponseEntity.ok(dashboard);
    }
}