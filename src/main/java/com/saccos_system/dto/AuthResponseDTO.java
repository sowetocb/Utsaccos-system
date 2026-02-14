package com.saccos_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class AuthResponseDTO {
    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String fullName;
    private String memberNumber;
    private String role;  // Primary role for backward compatibility
    private List<String> roles;
    private Long expiresIn;

    public AuthResponseDTO(String token, Long userId, String username,
                           String fullName, String memberNumber, String role, Long expiresIn) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.memberNumber = memberNumber;
        this.role = role;
        this.expiresIn = expiresIn;
        this.roles = List.of(role); // Default to single role list
    }

    // Constructor with all roles
    public AuthResponseDTO(String token, Long userId, String username,
                           String fullName, String memberNumber, List<String> roles, Long expiresIn) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.memberNumber = memberNumber;
        this.roles = roles;
        this.role = roles.contains("ADMIN") ? "ADMIN" :
                (roles.contains("LOAN_OFFICER") ? "LOAN_OFFICER" :
                        (roles.contains("ACCOUNTANT") ? "ACCOUNTANT" : "MEMBER"));
        this.expiresIn = expiresIn;
    }
}