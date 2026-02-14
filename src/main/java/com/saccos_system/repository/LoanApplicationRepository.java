package com.saccos_system.repository;

import com.saccos_system.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Loan Application Repository
@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByProfile_ProfileIdOrderByAppliedDateDesc(Long profileId);
    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);
    List<LoanApplication> findByStatus_StatusCode(String statusCode);
    List<LoanApplication> findByAppliedDateBetween(LocalDateTime start, LocalDateTime end);
}
