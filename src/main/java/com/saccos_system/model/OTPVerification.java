package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "OTPVerification")
@Data
public class OTPVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OtpId")
    private Long otpId;

    @Column(name = "IdNumber", nullable = false, length = 50)
    private String idNumber;

    @Column(name = "OtpCode", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "OtpType", nullable = false, length = 20)
    private String otpType; // REGISTRATION, PASSWORD_RESET

    @Column(name = "PhoneNumber", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "IsUsed")
    private Boolean isUsed = false;

    @Column(name = "ExpiryDate", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UsedDate")
    private LocalDateTime usedDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}