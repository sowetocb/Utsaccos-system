package com.saccos_system.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
@Slf4j
public class DebugController {

    @GetMapping("/security/deep")
    public Map<String, Object> deepSecurityCheck() {
        Map<String, Object> result = new HashMap<>();

        // Get the current thread name
        result.put("thread", Thread.currentThread().getName());

        // Get SecurityContext
        result.put("securityContext", SecurityContextHolder.getContext().getClass().getName());

        // Get Authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            result.put("error", "No authentication found");
            return result;
        }

        result.put("authenticated", auth.isAuthenticated());
        result.put("principalType", auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null");
        result.put("principal", auth.getPrincipal() != null ? auth.getPrincipal().toString() : "null");
        result.put("credentials", auth.getCredentials() != null ? "present" : "null");
        result.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        // Check for ROLE_ADMIN in different formats
        boolean hasRoleAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        result.put("hasRoleAdmin", hasRoleAdmin);

        boolean hasAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        result.put("hasAdmin", hasAdmin);

        // Get class details
        result.put("authClass", auth.getClass().getName());

        log.info("Deep security check: {}", result);

        return result;
    }

    @GetMapping("/test/members-access")
    public String testMembersAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Members access - Auth: {}", auth);
        return "Members access granted";
    }

    @GetMapping("/test/admin-access")
    public String testAdminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Admin access - Auth: {}", auth);
        return "Admin access granted";
    }
}