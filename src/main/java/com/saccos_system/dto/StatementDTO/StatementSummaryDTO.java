package com.saccos_system.dto.StatementDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StatementSummaryDTO {
    private String statementNumber;
    private String period;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private LocalDateTime generatedDate;
    private Boolean isSent;
}
