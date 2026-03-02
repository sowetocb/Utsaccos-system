package com.saccos_system.dto.AuthDTO;



import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class AuthRequestDTO {

    @NotBlank(message = "Username or ID Number is required")
    private String usernameOrIdNumber;

    @NotBlank(message = "Password is required")
    private String password;
}