package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    //  Emergency loan specific fields
    private String emergencyReason;
    private String supportingDocument;
    private Boolean documentVerified;
    private Boolean phoneVerified;

    // Guarantor information (for regular loans)
    private String guarantorIdNumber;
    private String guarantorName;
    private String guarantorPhone;
}