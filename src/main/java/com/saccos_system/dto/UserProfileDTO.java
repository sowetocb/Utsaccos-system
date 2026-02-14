package com.saccos_system.dto;



import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserProfileDTO {
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private Boolean isActive;
    private LocalDateTime lastLoginDate;
    private LocalDateTime passwordChangedDate;

    // Profile info
    private Long profileId;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String idNumber;
    private LocalDate dateOfBirth;
    private LocalDate joinDate;
    private String statusName;

    // Account info
    private String accountNumber;
    private Double savingsBalance;
    private Double loanBalance;
}