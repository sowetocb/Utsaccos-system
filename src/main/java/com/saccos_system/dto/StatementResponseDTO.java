package com.saccos_system.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StatementResponseDTO {
    private String statementNumber;
    private String period; // e.g., "January 2026"
    private String memberName;
    private String memberNumber;
    private String accountNumber;
    private BigDecimal openingBalance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal totalInterest;
    private BigDecimal closingBalance;
    private List<StatementTransactionDTO> transactions;
    private LocalDateTime generatedDate;
}

