package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminLoanApplicationDTO {
    private Long applicationId;
    private String applicationNumber;
    private String memberName;
    private String memberNumber;
    private String loanType;
    private BigDecimal amount;
    private Integer termMonths;
    private String purpose;
    private LocalDateTime appliedDate;
    private String status;
}
