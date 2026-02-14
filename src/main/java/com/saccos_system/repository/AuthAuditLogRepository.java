package com.saccos_system.repository;


import com.saccos_system.model.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {
    List<AuthAuditLog> findByUserIdOrderByCreatedDateDesc(Long userId);
    List<AuthAuditLog> findByActionAndCreatedDateBetween(String action, LocalDateTime start, LocalDateTime end);
}
