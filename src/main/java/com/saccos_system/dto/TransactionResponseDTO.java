package com.saccos_system.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponseDTO {
    private String transactionRef;
    private String type; // DEPOSIT, WITHDRAWAL, INTEREST, etc.
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime transactionDate;
    private String status; // SUCCESS, PENDING, FAILED
}