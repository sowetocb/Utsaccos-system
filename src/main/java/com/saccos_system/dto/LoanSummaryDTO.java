package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanSummaryDTO {
    private String loanNumber;
    private String loanType;
    private BigDecimal principalAmount;
    private BigDecimal remainingBalance;
    private BigDecimal monthlyPayment;
    private String status;
    private LocalDate nextPaymentDate;
}
