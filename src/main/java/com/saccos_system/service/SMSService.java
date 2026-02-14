package com.saccos_system.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SMSService {

    public void sendOTP(String phoneNumber, String otpCode) {
        // In production, integrate with SMS gateway like Twilio, AWS SNS, etc.
        log.info("SMS OTP sent to {}: {}", phoneNumber, otpCode);
        log.info("For demo purposes, OTP is: {}", otpCode);

        // Mock implementation - replace with actual SMS service
        // Example with Twilio:
        // Twilio.init(accountSid, authToken);
        // Message message = Message.creator(
        //     new PhoneNumber(phoneNumber),
        //     new PhoneNumber("+1234567890"),
        //     "Your OTP code is: " + otpCode
        // ).create();
    }
}