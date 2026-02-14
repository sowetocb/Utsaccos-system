package com.saccos_system.repository;

import com.saccos_system.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanNumber(String loanNumber);

    List<Loan> findByProfile_ProfileId(Long profileId);

    List<Loan> findByProfile_ProfileIdAndStatus_StatusCode(Long profileId, String statusCode);

    List<Loan> findByStatus_StatusCode(String statusCode);

    @Query("SELECT l FROM Loan l WHERE l.status.statusCode = :statusCode AND l.endDate < CURRENT_DATE")
    List<Loan> findOverdueLoans(@Param("statusCode") String statusCode);

    @Query("SELECT SUM(l.principalAmount) FROM Loan l")
    Optional<BigDecimal> getTotalLoanAmount();

    @Query("SELECT SUM(l.remainingBalance) FROM Loan l WHERE l.status.statusCode = 'LOAN_ACTIVE'")
    Optional<BigDecimal> getTotalOutstandingBalance();
}