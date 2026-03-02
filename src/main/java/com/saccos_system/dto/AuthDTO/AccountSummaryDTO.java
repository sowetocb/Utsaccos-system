package com.saccos_system.dto.AuthDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountSummaryDTO {
    private String memberNumber;
    private String memberName;
    private String accountNumber;
    private BigDecimal balance;

}
