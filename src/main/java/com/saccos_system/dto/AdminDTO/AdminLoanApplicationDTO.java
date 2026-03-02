package com.saccos_system.dto.AdminDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private BigDecimal approvedAmount;
    private String rejectionReason;

    // Emergency loan specific fields
    private String emergencyReason;
    private String supportingDocument;
    private Boolean documentVerified;
    private Boolean phoneVerified;
    private String verificationNotes;

    // Guarantor information
    private String guarantorIdNumber;
    private String guarantorName;
    private String guarantorPhone;
}