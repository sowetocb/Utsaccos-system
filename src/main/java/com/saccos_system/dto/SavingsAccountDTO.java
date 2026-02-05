package com.saccos_system.dto;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SavingsAccountDTO {
    private Long savingId;
    private String accountNumber;
    private Long profileId;  // Reference to member
    private String accountType;
    private BigDecimal balance;
    private BigDecimal interestRate;
    private LocalDate lastInterestCalcDate;
    private StatusDTO status;
    private LocalDateTime createdDate;
    private String createdBy;
}