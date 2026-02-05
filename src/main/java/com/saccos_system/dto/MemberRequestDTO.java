package com.saccos_system.dto;


import lombok.Data;
import java.time.LocalDate;

@Data
public class MemberRequestDTO {
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String idNumber;
    private LocalDate dateOfBirth;
    private Integer statusId;  // Only send status ID
    private String createdBy;
}