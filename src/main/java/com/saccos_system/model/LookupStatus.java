package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "LookupStatus")
@Data
public class LookupStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StatusID")
    private Integer statusId;

    @ManyToOne
    @JoinColumn(name = "CategoryID", nullable = false)
    private LookupCategory category;

    @Column(name = "StatusCode", nullable = false, length = 50, unique = true)
    private String statusCode;

    @Column(name = "StatusName", nullable = false, length = 100)
    private String statusName;

    @Column(name = "Description", length = 255)
    private String description;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}