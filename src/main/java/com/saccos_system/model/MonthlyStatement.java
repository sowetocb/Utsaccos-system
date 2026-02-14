package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MonthlyStatement")
@Data
public class MonthlyStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StatementID")
    private Long statementId;

    @Column(name = "StatementNumber", nullable = false, length = 50, unique = true)
    private String statementNumber;

    @ManyToOne
    @JoinColumn(name = "ProfileID", nullable = false)
    private StaffProfile profile;

    @Column(name = "Month", nullable = false)
    private Integer month;

    @Column(name = "Year", nullable = false)
    private Integer year;

    @Column(name = "OpeningBalance", nullable = false, precision = 18, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "TotalDeposits", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDeposits;

    @Column(name = "TotalWithdrawals", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalWithdrawals;

    @Column(name = "TotalInterest", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "ClosingBalance", nullable = false, precision = 18, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "GeneratedDate")
    private LocalDateTime generatedDate;

    @Column(name = "GeneratedBy", length = 100)
    private String generatedBy;

    @Column(name = "IsSent")
    private Boolean isSent = false;

    @Column(name = "SentDate")
    private LocalDateTime sentDate;

    @PrePersist
    protected void onCreate() {
        if (generatedDate == null) generatedDate = LocalDateTime.now();
    }
}