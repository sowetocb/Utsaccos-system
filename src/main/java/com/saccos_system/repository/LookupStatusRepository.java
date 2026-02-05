package com.saccos_system.repository;

import com.saccos_system.domain.LookupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LookupStatusRepository extends JpaRepository<LookupStatus, Integer> {
    Optional<LookupStatus> findByStatusCode(String statusCode);
    List<LookupStatus> findByCategory_CategoryName(String categoryName);
}