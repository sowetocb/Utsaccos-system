package com.saccos_system.dto.LoanDTO;

import lombok.Data;

@Data
public class LoanRejectionDTO {
    private String rejectionReason;
    private String rejectedBy;
}
