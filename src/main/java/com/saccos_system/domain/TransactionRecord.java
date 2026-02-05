package com.saccos_system.domain;



import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TransactionRecord")
@Data
public class TransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionID")
    private Long transactionId;

    @Column(name = "TransactionRef", unique = true, nullable = false, length = 100)
    private String transactionRef;

    @ManyToOne
    @JoinColumn(name = "SavingID")
    private SavingsAccount savingsAccount;

    @Column(name = "TransactionType", length = 50)
    private String transactionType;

    @Column(name = "Amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "BalanceBefore", precision = 18, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "BalanceAfter", precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "Description", length = 500)
    private String description;

    @Column(name = "TransactionDate")
    private LocalDateTime transactionDate;

    @Column(name = "PerformedBy", length = 100)
    private String performedBy;

    @PrePersist
    protected void onCreate() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
    }
}