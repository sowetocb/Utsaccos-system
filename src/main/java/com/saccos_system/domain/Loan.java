package com.saccos_system.domain;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Loan")
@Data
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LoanID")
    private Long loanId;

    @Column(name = "LoanNumber", unique = true, nullable = false, length = 50)
    private String loanNumber;

    @ManyToOne
    @JoinColumn(name = "ProfileID")
    private StaffProfile profile;

    @Column(name = "PrincipalAmount", precision = 18, scale = 2, nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "InterestRate", precision = 5, scale = 2, nullable = false)
    private BigDecimal interestRate;

    @Column(name = "LoanType", length = 50)
    private String loanType;

    @Column(name = "TermMonths", nullable = false)
    private Integer termMonths;

    @Column(name = "StartDate")
    private LocalDate startDate;

    @Column(name = "EndDate")
    private LocalDate endDate;

    @Column(name = "MonthlyPayment", precision = 18, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(name = "RemainingBalance", precision = 18, scale = 2)
    private BigDecimal remainingBalance;

    @ManyToOne
    @JoinColumn(name = "StatusID")
    private LookupStatus status;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "CreatedBy", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}
