package com.saccos_system.repository;

import com.saccos_system.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {
    List<TransactionRecord> findBySavingsAccount_SavingIdOrderByTransactionDateDesc(Long savingId);
    @Query("SELECT t FROM TransactionRecord t WHERE t.savingsAccount.savingId = :savingId ORDER BY t.transactionDate DESC LIMIT :limit")
    List<TransactionRecord> findTopNBySavingsAccount_SavingIdOrderByTransactionDateDesc(@Param("savingId") Long savingId, @Param("limit") Integer limit);
    List<TransactionRecord> findBySavingsAccount_SavingIdAndTransactionDateAfterOrderByTransactionDateDesc(Long savingId, LocalDateTime date);
    List<TransactionRecord> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);
    List<TransactionRecord> findBySavingsAccount_SavingIdAndTransactionDateBetweenOrderByTransactionDate(Long savingId, LocalDateTime start, LocalDateTime end);
    Optional<TransactionRecord> findTopBySavingsAccount_SavingIdAndTransactionDateBeforeOrderByTransactionDateDesc(Long savingId, LocalDateTime date);
    @Query("SELECT t FROM TransactionRecord t " +
            "WHERE t.savingsAccount.profile.profileId = :profileId " +
            "ORDER BY t.transactionDate DESC " +
            "LIMIT 5")
    List<TransactionRecord> findTop5BySavingsAccount_Profile_ProfileIdOrderByTransactionDateDesc(@Param("profileId") Long profileId);
    List<TransactionRecord> findBySavingsAccount_Profile_ProfileIdOrderByTransactionDateDesc(Long profileId);
    List<TransactionRecord> findByTransactionTypeAndTransactionDateBetween(String transactionType, LocalDateTime start, LocalDateTime end);
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionRecord t " +
            "WHERE t.savingsAccount.profile.profileId = :profileId " +
            "AND t.transactionType = 'DEPOSIT'")
    BigDecimal getTotalDepositsByProfileId(@Param("profileId") Long profileId);
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionRecord t " +
            "WHERE t.savingsAccount.profile.profileId = :profileId " +
            "AND t.transactionType = 'WITHDRAWAL'")
    BigDecimal getTotalWithdrawalsByProfileId(@Param("profileId") Long profileId);
}