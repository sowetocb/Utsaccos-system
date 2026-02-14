package com.saccos_system.repository;
import com.saccos_system.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


    @Repository
    public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {
        Optional<SystemUser> findByUsername(String username);
        Optional<SystemUser> findByEmail(String email);
        Optional<SystemUser> findByProfile_ProfileId(Long profileId);
        Optional<SystemUser> findByProfile_IdNumber(String idNumber);
        boolean existsByUsername(String username);
        boolean existsByProfile_IdNumber(String idNumber);

    }


