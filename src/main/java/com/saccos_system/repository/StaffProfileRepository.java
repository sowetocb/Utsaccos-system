package com.saccos_system.repository;

import com.saccos_system.domain.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    Optional<StaffProfile> findByMemberNumber(String memberNumber);
    Optional<StaffProfile> findByEmail(String email);
    Optional<StaffProfile> findByIdNumber(String idNumber);
    boolean existsByMemberNumber(String memberNumber);
    boolean existsByEmail(String email);
}