package com.saccos_system.controller;

import com.saccos_system.dto.*;
import com.saccos_system.service.SavingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings")
@RequiredArgsConstructor
@Tag(name = "Savings Management", description = "Savings account operations")
@SecurityRequirement(name = "bearerAuth")
public class SavingsController {

    private final SavingsService savingsService;

    @PostMapping("/deposit")
    @Operation(summary = "Make a deposit to savings account")
    public ResponseEntity<TransactionResponseDTO> deposit(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody DepositRequestDTO request) {
        TransactionResponseDTO response = savingsService.deposit(token, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Make a withdrawal from savings account")
    public ResponseEntity<TransactionResponseDTO> withdraw(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody WithdrawalRequestDTO request) {
        TransactionResponseDTO response = savingsService.withdraw(token, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get transaction history")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactions(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer limit) {
        List<TransactionResponseDTO> transactions = savingsService.getTransactionHistory(token, limit);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get savings account summary")
    public ResponseEntity<SavingsSummaryDTO> getSummary(
            @RequestHeader("Authorization") String token) {
        SavingsSummaryDTO summary = savingsService.getSavingsSummary(token);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/balance")
    @Operation(summary = "Get current savings balance")
    public ResponseEntity<BalanceResponseDTO> getBalance(
            @RequestHeader("Authorization") String token) {
        SavingsSummaryDTO summary = savingsService.getSavingsSummary(token);
        BalanceResponseDTO response = new BalanceResponseDTO();
        response.setAccountNumber(summary.getAccountNumber());
        response.setCurrentBalance(summary.getCurrentBalance());
        response.setAccountStatus(summary.getAccountStatus());
        return ResponseEntity.ok(response);
    }
}