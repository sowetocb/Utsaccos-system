package com.saccos_system.repository;

import com.saccos_system.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
    public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
        boolean existsByToken(String token);
        void deleteByExpiryDateBefore(LocalDateTime date);
    }
