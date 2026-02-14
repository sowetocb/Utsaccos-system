package com.saccos_system.repository;

import com.saccos_system.model.MonthlyStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyStatementRepository extends JpaRepository<MonthlyStatement, Long> {
    List<MonthlyStatement> findByProfile_ProfileIdOrderByYearDescMonthDesc(Long profileId);
    Optional<MonthlyStatement> findByStatementNumber(String statementNumber);
    Optional<MonthlyStatement> findByProfile_ProfileIdAndMonthAndYear(Long profileId, Integer month, Integer year);
    List<MonthlyStatement> findByIsSentFalseAndGeneratedDateBefore(LocalDateTime date);
}
