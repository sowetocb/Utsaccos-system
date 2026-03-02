package com.saccos_system.dto.LoanDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanDetailsDTO {
    private String loanNumber;
    private String loanType;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyPayment;
    private BigDecimal remainingBalance;
    private BigDecimal totalPaid;
    private String status;
}
