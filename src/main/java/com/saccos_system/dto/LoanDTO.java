package com.saccos_system.dto;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LoanDTO {
    private Long loanId;
    private String loanNumber;
    private Long profileId;  // Reference to member
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private String loanType;
    private Integer termMonths;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyPayment;
    private BigDecimal remainingBalance;
    private StatusDTO status;
    private LocalDateTime createdDate;
    private String createdBy;
}