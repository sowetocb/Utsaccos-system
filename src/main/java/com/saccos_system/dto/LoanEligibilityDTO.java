package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanEligibilityDTO {
    private Boolean isEligible;
    private BigDecimal maxEligibleAmount;
    private String reason;
}
