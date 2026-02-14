package com.saccos_system.schedule;


import com.saccos_system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCleanupScheduler {

    private final AuthService authService;

    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void cleanupExpiredTokens() {
        authService.cleanupExpiredTokens();
    }
}