package com.saccos_system.repository;


import com.saccos_system.model.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {
    Optional<SavingsAccount> findByAccountNumber(String accountNumber);
    Optional<SavingsAccount> findByProfile_ProfileId(Long profileId);
    boolean existsByAccountNumber(String accountNumber);
}