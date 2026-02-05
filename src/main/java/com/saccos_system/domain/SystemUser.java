package com.saccos_system.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "SystemUser")
@Data
public class SystemUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserId")
    private Long userId;

    @OneToOne
    @JoinColumn(name = "ProfileID")
    private StaffProfile profile;

    @Column(name = "Username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "PasswordHash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "IsLocked")
    private Boolean isLocked = false;

    @Column(name = "FailedLoginAttempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "LastLoginDate")
    private LocalDateTime lastLoginDate;

    @Column(name = "PasswordChangedDate")
    private LocalDateTime passwordChangedDate;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "CreatedBy", length = 100)
    private String createdBy;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Column(name = "ModifiedBy", length = 100)
    private String modifiedBy;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedDate = LocalDateTime.now();
    }
}