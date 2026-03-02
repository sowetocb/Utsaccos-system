package com.saccos_system.dto.SavingsDTO;

import com.saccos_system.dto.AuthDTO.AccountSummaryDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SavingsReportDTO {
    private Integer totalAccounts;
    private BigDecimal totalBalance;
    private BigDecimal monthlyDeposits;
    private BigDecimal monthlyWithdrawals;
    private List<AccountSummaryDTO> topAccounts;
}
