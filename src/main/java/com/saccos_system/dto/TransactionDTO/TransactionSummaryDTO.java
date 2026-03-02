package com.saccos_system.dto.TransactionDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionSummaryDTO {
    private String transactionRef;
    private String memberName;
    private String transactionType;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
}
