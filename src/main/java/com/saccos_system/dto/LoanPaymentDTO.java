package com.saccos_system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanPaymentDTO {

    @NotBlank(message = "Loan number is required")
    private String loanNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // CASH, BANK_TRANSFER, MOBILE_MONEY

    private String transactionReference;

    private String notes;

    private Boolean payFullBalance = false;
}