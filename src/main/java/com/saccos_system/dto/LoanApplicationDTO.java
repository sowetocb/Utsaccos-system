package com.saccos_system.dto;

import com.saccos_system.model.LoanType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanApplicationDTO {

    @NotNull(message = "Loan type is required")
    private LoanType loanType;
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum loan amount is 1,000")
    @DecimalMax(value = "1000000.00", message = "Maximum loan amount is 1,000,000")
    private BigDecimal amount;
    @NotNull(message = "Term is required")
    @Min(value = 1, message = "Minimum term is 1 month")
    @Max(value = 60, message = "Maximum term is 60 months")
    private Integer termMonths;
    @NotBlank(message = "Purpose is required")
    @Size(max = 500, message = "Purpose cannot exceed 500 characters")
    private String purpose;
    private String emergencyReason;
    private String supportingDocument;

    // Regular loan fields
    private String guarantorIdNumber;
    private String guarantorName;
    private String guarantorPhone;
}