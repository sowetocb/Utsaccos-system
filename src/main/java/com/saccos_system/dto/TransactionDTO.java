package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionDTO {
    private String date;
    private String description;
    private BigDecimal amount;
    private String type; // DEPOSIT, WITHDRAWAL, INTEREST, FEE
    private BigDecimal balanceAfter;
}