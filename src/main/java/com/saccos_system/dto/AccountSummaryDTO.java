package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountSummaryDTO {
    private String memberNumber;
    private String memberName;
    private String accountNumber;
    private BigDecimal balance;

}
