package com.saccos_system.dto;


import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AdminDashboardDTO {
    private Integer totalMembers;
    private Integer activeMembers;
    private BigDecimal totalSavings;
    private BigDecimal totalLoans;
    private Integer pendingApplications;
    private Integer overdueLoans;
    private Integer totalAdmins;
    private Integer totalLoanOfficers;
    private Integer totalAccountants;
    private List<DashboardChartDTO> savingsTrend;
    private List<DashboardChartDTO> loansTrend;
    private Integer pendingEmergencyApplications;

}