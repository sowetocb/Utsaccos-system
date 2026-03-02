package com.saccos_system.dto.LoanDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LoansReportDTO {
    private Integer totalLoans;
    private Integer activeLoans;
    private BigDecimal totalDisbursed;
    private BigDecimal totalRepaid;
    private BigDecimal outstandingBalance;
    private List<LoanSummaryDTO> activeLoanDetails;
    private Integer emergencyLoans;
}
