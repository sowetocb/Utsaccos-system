package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "LoanApplication")
@Data
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ApplicationID")
    private Long applicationId;

    @Column(name = "ApplicationNumber", nullable = false, length = 50, unique = true)
    private String applicationNumber;

    @ManyToOne
    @JoinColumn(name = "ProfileID", nullable = false)
    private StaffProfile profile;

    @Column(name = "LoanType", nullable = false, length = 50)
    private String loanType;

    @Column(name = "Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "TermMonths", nullable = false)
    private Integer termMonths;

    @Column(name = "Purpose", length = 500)
    private String purpose;

    @Column(name = "AppliedDate")
    private LocalDateTime appliedDate;

    @ManyToOne
    @JoinColumn(name = "StatusID", nullable = false)
    private LookupStatus status;

    @Column(name = "ApprovedDate")
    private LocalDateTime approvedDate;

    @Column(name = "ApprovedBy", length = 100)
    private String approvedBy;

    @Column(name = "ApprovedAmount", precision = 18, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "RejectionReason", length = 500)
    private String rejectionReason;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "CreatedBy", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
        if (appliedDate == null) appliedDate = LocalDateTime.now();
    }
}
