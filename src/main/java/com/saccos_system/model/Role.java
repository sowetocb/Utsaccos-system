package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Roles")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleID")
    private Integer roleId;
    @Column(name = "RoleName", nullable = false, length = 50, unique = true)
    private String roleName;
    @Column(name = "Description", length = 255)
    private String description;
    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}