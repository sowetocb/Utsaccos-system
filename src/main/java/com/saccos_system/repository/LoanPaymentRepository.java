package com.saccos_system.repository;

import com.saccos_system.model.LoanPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanPaymentRepository extends JpaRepository<LoanPayment, Long> {
    List<LoanPayment> findByLoan_LoanIdOrderByPaymentDateDesc(Long loanId);
    Optional<LoanPayment> findByPaymentReference(String paymentReference);
    List<LoanPayment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);
}
