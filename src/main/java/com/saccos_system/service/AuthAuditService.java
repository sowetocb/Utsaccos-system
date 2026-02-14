package com.saccos_system.service;


import com.saccos_system.model.AuthAuditLog;
import com.saccos_system.repository.AuthAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthAuditService {

    private final AuthAuditLogRepository auditLogRepository;

    public void logAuthEvent(Long userId, String idNumber, String action,
                             String status, String errorMessage, HttpServletRequest request) {
        AuthAuditLog auditLog = new AuthAuditLog();
        auditLog.setUserId(userId);
        auditLog.setIdNumber(idNumber);
        auditLog.setAction(action);
        auditLog.setStatus(status);
        auditLog.setErrorMessage(errorMessage);

        if (request != null) {
            auditLog.setIpAddress(getClientIp(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }

        auditLogRepository.save(auditLog);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }
}