package com.saccos_system.dto.StatementDTO;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StatementTransactionDTO {
    private String date;
    private String description;
    private String reference;
    private BigDecimal deposit;
    private BigDecimal withdrawal;
    private BigDecimal balance;
}