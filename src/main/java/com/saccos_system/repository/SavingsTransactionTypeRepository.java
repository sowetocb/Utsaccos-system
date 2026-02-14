package com.saccos_system.repository;

import com.saccos_system.model.SavingsTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavingsTransactionTypeRepository extends JpaRepository<SavingsTransactionType, Integer> {

    Optional<SavingsTransactionType> findByTypeCode(String typeCode);

    Optional<SavingsTransactionType> findByTypeName(String typeName);

    boolean existsByTypeCode(String typeCode);
}