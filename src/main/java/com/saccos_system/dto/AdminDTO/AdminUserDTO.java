package com.saccos_system.dto.AdminDTO;


import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserDTO {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String memberNumber;
    private Boolean isActive;
    private Boolean isLocked;
    private LocalDateTime lastLoginDate;
    private LocalDateTime createdDate;
    private List<String> roles;
}