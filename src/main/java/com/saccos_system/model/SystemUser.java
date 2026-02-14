package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<UserRole> userRoles = new ArrayList<>();

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

//check specific user role
    public boolean hasRole(String roleName) {
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        return userRoles.stream()
                .anyMatch(ur -> ur.getRole() != null &&
                        roleName.equals(ur.getRole().getRoleName()));
    }
    //get all role names
    public List<String> getRoleNames() {
        if (userRoles == null || userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        return userRoles.stream()
                .filter(ur -> ur.getRole() != null)
                .map(ur -> ur.getRole().getRoleName())
                .collect(Collectors.toList());
    }
}