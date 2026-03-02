package com.saccos_system.dto.UserDTO;


import com.saccos_system.dto.AdminDTO.StatusDTO;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MemberResponseDTO {
    private Long profileId;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String idNumber;
    private LocalDate dateOfBirth;
    private LocalDate joinDate;
    private StatusDTO status;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime modifiedDate;
    private String modifiedBy;
}

