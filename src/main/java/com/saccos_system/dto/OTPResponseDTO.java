package com.saccos_system.dto;


import lombok.Data;

@Data
public class OTPResponseDTO {
    private String message;
    private String phoneNumber; // Masked phone number
    private Integer otpExpiryMinutes;
    public OTPResponseDTO(String message, String phoneNumber, Integer otpExpiryMinutes) {
        this.message = message;
        this.phoneNumber = maskPhoneNumber(phoneNumber);
        this.otpExpiryMinutes = otpExpiryMinutes;
    }
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return "***" + phone.substring(phone.length() - 4);
    }
}