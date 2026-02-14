package com.saccos_system.model;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "AuthAuditLog")
@Data
public class AuthAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AuditId")
    private Long auditId;

    @Column(name = "UserId")
    private Long userId;

    @Column(name = "IdNumber", length = 50)
    private String idNumber;

    @Column(name = "Action", nullable = false, length = 50)
    private String action;

    @Column(name = "IpAddress", length = 50)
    private String ipAddress;

    @Column(name = "UserAgent", length = 500)
    private String userAgent;

    @Column(name = "Status", nullable = false, length = 20)
    private String status;

    @Column(name = "ErrorMessage", length = 500)
    private String errorMessage;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}