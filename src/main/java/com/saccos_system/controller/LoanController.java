package com.saccos_system.controller;



import com.saccos_system.dto.LoanDTO.*;
import com.saccos_system.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Management", description = "Loan operations and applications")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    @Operation(summary = "Apply for a new loan")
    public ResponseEntity<LoanApplicationResponseDTO> applyForLoan(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody LoanApplicationDTO request) {
        LoanApplicationResponseDTO response = loanService.applyForLoan(token, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/applications")
    @Operation(summary = "Get my loan applications")
    public ResponseEntity<List<LoanApplicationResponseDTO>> getMyApplications(
            @RequestHeader("Authorization") String token) {
        List<LoanApplicationResponseDTO> applications = loanService.getMyLoanApplications(token);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/my-loans")
    @Operation(summary = "Get my active and past loans")
    public ResponseEntity<List<LoanSummaryDTO>> getMyLoans(
            @RequestHeader("Authorization") String token) {
        List<LoanSummaryDTO> loans = loanService.getMyLoans(token);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{loanNumber}")
    @Operation(summary = "Get loan details")
    public ResponseEntity<LoanDetailsDTO> getLoanDetails(
            @RequestHeader("Authorization") String token,
            @PathVariable String loanNumber) {
        LoanDetailsDTO details = loanService.getLoanDetails(token, loanNumber);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/{loanNumber}/pay")
    @Operation(summary = "Make a loan payment")
    public ResponseEntity<LoanPaymentResponseDTO> makePayment(
            @RequestHeader("Authorization") String token,
            @PathVariable String loanNumber,
            @Valid @RequestBody LoanPaymentDTO request) {
        request.setLoanNumber(loanNumber);
        LoanPaymentResponseDTO response = loanService.makePayment(token, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{loanNumber}/payments")
    @Operation(summary = "Get loan payment history")
    public ResponseEntity<List<LoanPaymentResponseDTO>> getPaymentHistory(
            @RequestHeader("Authorization") String token,
            @PathVariable String loanNumber) {
        List<LoanPaymentResponseDTO> payments = loanService.getPaymentHistory(token, loanNumber);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{loanNumber}/schedule")
    @Operation(summary = "Get loan payment schedule")
    public ResponseEntity<LoanScheduleDTO> getPaymentSchedule(
            @RequestHeader("Authorization") String token,
            @PathVariable String loanNumber) {
        LoanScheduleDTO schedule = loanService.getPaymentSchedule(token, loanNumber);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/eligibility")
    @Operation(summary = "Check loan eligibility")
    public ResponseEntity<LoanEligibilityDTO> checkEligibility(
            @RequestHeader("Authorization") String token) {
        // This would call a method to check eligibility
        LoanEligibilityDTO eligibility = new LoanEligibilityDTO();
        eligibility.setIsEligible(true);
        eligibility.setMaxEligibleAmount(BigDecimal.valueOf(500000));
        eligibility.setReason("Based on your savings balance");
        return ResponseEntity.ok(eligibility);
    }
}

