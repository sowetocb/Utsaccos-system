package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Additional DTOs for Loan Service
@Data
public class LoanApplicationResponseDTO {
    private String applicationNumber;
    private String loanType;
    private BigDecimal amount;
    private Integer termMonths;
    private String purpose;
    private LocalDateTime appliedDate;
    private String status;
    private BigDecimal approvedAmount;
    private String rejectionReason;
}
