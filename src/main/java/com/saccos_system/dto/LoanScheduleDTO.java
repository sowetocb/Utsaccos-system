package com.saccos_system.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LoanScheduleDTO {
    private String loanNumber;
    private Integer totalInstallments;
    private BigDecimal totalDue;
    private BigDecimal totalPaid;
    private BigDecimal remainingBalance;
    private List<ScheduleItemDTO> schedule;
}
