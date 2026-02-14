package com.saccos_system.repository;

import com.saccos_system.model.LoanPaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanPaymentScheduleRepository extends JpaRepository<LoanPaymentSchedule, Long> {
    List<LoanPaymentSchedule> findByLoan_LoanIdOrderByDueDate(Long loanId);
    List<LoanPaymentSchedule> findByLoan_LoanIdAndStatusOrderByDueDate(Long loanId, String status);
    List<LoanPaymentSchedule> findByDueDateAndStatus(LocalDate dueDate, String status);
    Optional<LoanPaymentSchedule> findByLoan_LoanIdAndInstallmentNumber(Long loanId, Integer installmentNumber);
}