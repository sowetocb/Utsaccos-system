package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "UserRoles")
@Data
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserRoleID")
    private Long userRoleId;
    @ManyToOne
    @JoinColumn(name = "UserID", nullable = false)
    private SystemUser user;
    @ManyToOne
    @JoinColumn(name = "RoleID", nullable = false)
    private Role role;
    @Column(name = "AssignedDate")
    private LocalDateTime assignedDate;
    @Column(name = "AssignedBy", length = 100)
    private String assignedBy;
    @PrePersist
    protected void onCreate() {
        if (assignedDate == null) {
            assignedDate = LocalDateTime.now();
        }
    }
}