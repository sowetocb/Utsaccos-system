package com.saccos_system.repository;

import com.saccos_system.model.OTPVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
    public interface OTPVerificationRepository extends JpaRepository<OTPVerification, Long> {
        Optional<OTPVerification> findByIdNumberAndOtpCodeAndOtpTypeAndIsUsedFalse(
                String idNumber, String otpCode, String otpType);
        List<OTPVerification> findByIdNumberAndOtpTypeAndIsUsedFalseOrderByCreatedDateDesc(
                String idNumber, String otpType);
        int countByIdNumberAndOtpTypeAndCreatedDateAfter(
                String idNumber, String otpType, LocalDateTime afterDate);
    }
