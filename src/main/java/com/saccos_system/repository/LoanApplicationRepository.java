package com.saccos_system.repository;

import com.saccos_system.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;



@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    // Find by profile ID
    List<LoanApplication> findByProfile_ProfileIdOrderByAppliedDateDesc(Long profileId);
    // Find by application number
    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);
    // Find by status code
    List<LoanApplication> findByStatus_StatusCode(String statusCode);
    //  Find by status code ordered by applied date ascending (for priority queue)
    List<LoanApplication> findByStatus_StatusCodeOrderByAppliedDateAsc(String statusCode);
    // Find by applied date range
    List<LoanApplication> findByAppliedDateBetween(LocalDateTime start, LocalDateTime end);
    //  Find emergency loans by profile ID and date range (for checking recent emergency loans)
    List<LoanApplication> findByProfile_ProfileIdAndLoanTypeAndAppliedDateAfter(
            Long profileId, String loanType, LocalDateTime afterDate);
    //  Count pending emergency applications (for dashboard)
    @Query("SELECT COUNT(l) FROM LoanApplication l WHERE l.status.statusCode = :statusCode")
    long countByStatus_StatusCode(@Param("statusCode") String statusCode);
    //  Find applications by multiple status codes (for admin filtering)
    @Query("SELECT l FROM LoanApplication l WHERE l.status.statusCode IN :statusCodes ORDER BY l.appliedDate DESC")
    List<LoanApplication> findByStatus_StatusCodeInOrderByAppliedDateDesc(
            @Param("statusCodes") List<String> statusCodes);
}