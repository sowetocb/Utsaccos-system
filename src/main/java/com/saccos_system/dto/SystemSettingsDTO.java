package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SystemSettingsDTO {
    private BigDecimal minimumSavingsBalance;
    private BigDecimal maximumDailyWithdrawal;
    private BigDecimal maximumDailyDeposit;
    private BigDecimal loanInterestRate;
    private Integer loanTermMonths;
    private Boolean maintenanceMode;
    private String systemMessage;
}
