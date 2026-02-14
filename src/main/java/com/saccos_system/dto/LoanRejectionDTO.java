package com.saccos_system.dto;

import lombok.Data;

@Data
public class LoanRejectionDTO {
    private String rejectionReason;
    private String rejectedBy;
}
