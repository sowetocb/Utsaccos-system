package com.saccos_system.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "StaffProfile")
@Data
public class StaffProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProfileID")
    private Long profileId;
    @Column(name = "MemberNumber", unique = true, nullable = false, length = 50)
    private String memberNumber;
    @Column(name = "FirstName", nullable = false, length = 100)
    private String firstName;
    @Column(name = "LastName", nullable = false, length = 100)
    private String lastName;
    @Column(name = "Email", unique = true, length = 100)
    private String email;
    @Column(name = "Phone", length = 20)
    private String phone;
    @Column(name = "IdNumber", unique = true, length = 50)
    private String idNumber;
    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;
    @Column(name = "JoinDate")
    private LocalDate joinDate;
    @ManyToOne
    @JoinColumn(name = "StatusID")
    private LookupStatus status;
    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;
    @Column(name = "CreatedBy", length = 100)
    private String createdBy;
    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;
    @Column(name = "ModifiedBy", length = 100)
    private String modifiedBy;
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<SavingsAccount> savingsAccounts = new ArrayList<>();
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL)
    private List<Loan> loans = new ArrayList<>();
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        if (joinDate == null) {
            joinDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }
}