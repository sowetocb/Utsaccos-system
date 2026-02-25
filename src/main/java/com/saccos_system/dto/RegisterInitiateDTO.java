package com.saccos_system.dto;



import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class RegisterInitiateDTO {
    @NotBlank(message = "ID Number is required")
    @Pattern(regexp = "\\d+", message = "ID Number must contain only digits")
    private String idNumber;
}