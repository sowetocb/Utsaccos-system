package com.saccos_system.dto.TransactionDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TransactionsReportDTO {
    private Integer totalTransactions;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private List<TransactionSummaryDTO> recentTransactions;
}
