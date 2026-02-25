package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "LoanPaymentSchedule")
@Data
public class LoanPaymentSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ScheduleID")
    private Long scheduleId;
    @ManyToOne
    @JoinColumn(name = "LoanID", nullable = false)
    private Loan loan;
    @Column(name = "InstallmentNumber", nullable = false)
    private Integer installmentNumber;
    @Column(name = "DueDate", nullable = false)
    private LocalDate dueDate;
    @Column(name = "AmountDue", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountDue;
    @Column(name = "PrincipalAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalAmount;
    @Column(name = "InterestAmount", nullable = false, precision = 18, scale = 2)
    private BigDecimal interestAmount;
    @Column(name = "PaidDate")
    private LocalDate paidDate;
    @Column(name = "PaidAmount", precision = 18, scale = 2)
    private BigDecimal paidAmount;
    @Column(name = "Status", nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, PAID, OVERDUE
    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
    }
}