package com.saccos_system.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateProfileDTO {
    @Email(message = "Invalid email format")
    private String email;
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;
}