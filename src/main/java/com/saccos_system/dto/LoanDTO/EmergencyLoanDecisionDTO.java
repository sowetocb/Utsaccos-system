package com.saccos_system.dto.LoanDTO;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class EmergencyLoanDecisionDTO {
    private boolean approved;
    private BigDecimal approvedAmount;
    private String reason;
    private boolean documentVerified;
    private boolean phoneVerified;
    private String verificationNotes;
}