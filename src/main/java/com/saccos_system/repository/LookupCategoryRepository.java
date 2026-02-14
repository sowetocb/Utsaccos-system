package com.saccos_system.repository;

import com.saccos_system.model.LookupCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LookupCategoryRepository extends JpaRepository<LookupCategory, Integer> {
    Optional<LookupCategory> findByCategoryName(String categoryName);
}