package com.saccos_system.model;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SavingsAccount")
@Data
public class SavingsAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SavingID")
    private Long savingId;
    @Column(name = "AccountNumber", unique = true, nullable = false, length = 50)
    private String accountNumber;
    @ManyToOne
    @JoinColumn(name = "ProfileID")
    private StaffProfile profile;
    @Column(name = "AccountType", length = 50)
    private String accountType;
    @Column(name = "Balance", precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    @Column(name = "InterestRate", precision = 5, scale = 2)
    private BigDecimal interestRate;
    @Column(name = "LastInterestCalcDate")
    private LocalDate lastInterestCalcDate;
    @ManyToOne
    @JoinColumn(name = "StatusID")
    private LookupStatus status;
    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;
    @Column(name = "CreatedBy", length = 100)
    private String createdBy;
    @OneToMany(mappedBy = "savingsAccount", cascade = CascadeType.ALL)
    private List<TransactionRecord> transactions = new ArrayList<>();
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
    }
}