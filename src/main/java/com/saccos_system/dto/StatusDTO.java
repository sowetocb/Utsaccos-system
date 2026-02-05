package com.saccos_system.dto;


import lombok.Data;

@Data
public class StatusDTO {
    private Integer statusId;
    private String statusCode;
    private String statusName;
    private String description;
    private Boolean isActive;
}