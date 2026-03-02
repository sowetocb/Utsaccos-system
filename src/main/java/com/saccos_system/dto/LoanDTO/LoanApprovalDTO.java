package com.saccos_system.dto.LoanDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApprovalDTO {
    private BigDecimal approvedAmount;
    private BigDecimal interestRate;
    private String approvedBy;
    private String notes;
}
