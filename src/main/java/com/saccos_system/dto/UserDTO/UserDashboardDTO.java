package com.saccos_system.dto.UserDTO;


import com.saccos_system.dto.NotificationDTO.NotificationDTO;
import com.saccos_system.dto.TransactionDTO.TransactionDTO;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class UserDashboardDTO {

    private String memberName;
    private String memberNumber;
    private String memberSince;
    // Savings Summary
    private BigDecimal totalSavings;
    private BigDecimal lastMonthSavings;
    private BigDecimal interestEarned;
    private String savingsAccountStatus;

    // Loans Summary
    private Integer activeLoans;
    private BigDecimal totalLoanBalance;
    private BigDecimal nextPaymentAmount;
    private String nextPaymentDate;
    private BigDecimal totalPaid;

    // Recent Transactions
    private List<TransactionDTO> recentTransactions;

    // Notifications
    private List<NotificationDTO> unreadNotifications;

    // Quick Actions
    private List<String> availableActions;
}


