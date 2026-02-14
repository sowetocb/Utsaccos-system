package com.saccos_system.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanPaymentResponseDTO {
    private String paymentReference;
    private String loanNumber;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String transactionReference;
}
