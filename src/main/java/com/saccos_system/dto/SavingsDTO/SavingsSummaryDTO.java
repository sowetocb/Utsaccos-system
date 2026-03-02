package com.saccos_system.dto.SavingsDTO;

import com.saccos_system.dto.TransactionDTO.TransactionResponseDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SavingsSummaryDTO {
    private String accountNumber;
    private BigDecimal currentBalance;
    private BigDecimal interestRate;
    private String accountType;
    private String accountStatus;
    private BigDecimal estimatedMonthlyInterest;
    private List<TransactionResponseDTO> recentTransactions;
}
