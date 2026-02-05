package com.saccos_system.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "BlacklistedToken")
@Data
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TokenId")
    private Long tokenId;

    @Column(name = "Token", nullable = false, length = 1000, unique = true)
    private String token;

    @Column(name = "BlacklistedDate")
    private LocalDateTime blacklistedDate;

    @Column(name = "ExpiryDate", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "Reason", length = 100)
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (blacklistedDate == null) {
            blacklistedDate = LocalDateTime.now();
        }
    }
}