package com.saccos_system.dto.SavingsDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceResponseDTO {
    private String accountNumber;
    private BigDecimal currentBalance;
    private String accountStatus;
}
