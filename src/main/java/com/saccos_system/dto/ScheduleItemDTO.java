package com.saccos_system.dto;




import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScheduleItemDTO {
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal amountDue;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal paidAmount;
    private LocalDate paidDate;
    private String status;
}
