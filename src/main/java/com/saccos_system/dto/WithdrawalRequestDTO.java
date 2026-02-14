package com.saccos_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class WithdrawalRequestDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Withdrawal method is required")
    private String withdrawalMethod; // CASH, BANK_TRANSFER, MOBILE_MONEY

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    private String notes;
}