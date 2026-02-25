package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LoanPayment")
@Data
public class LoanPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentID")
    private Long paymentId;
    @Column(name = "PaymentReference", nullable = false, length = 100, unique = true)
    private String paymentReference;
    @ManyToOne
    @JoinColumn(name = "LoanID", nullable = false)
    private Loan loan;
    @ManyToOne
    @JoinColumn(name = "ScheduleID")
    private LoanPaymentSchedule schedule;
    @Column(name = "Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    @Column(name = "PaymentDate")
    private LocalDateTime paymentDate;
    @Column(name = "PaymentMethod", length = 50)
    private String paymentMethod; // CASH, BANK_TRANSFER, MOBILE_MONEY
    @Column(name = "TransactionReference", length = 100)
    private String transactionReference;
    @Column(name = "PaidBy", length = 100)
    private String paidBy;
    @Column(name = "Notes", length = 500)
    private String notes;
    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;
    @Column(name = "CreatedBy", length = 100)
    private String createdBy;
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
        if (paymentDate == null) paymentDate = LocalDateTime.now();
    }
}
