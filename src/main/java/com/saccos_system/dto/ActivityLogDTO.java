package com.saccos_system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityLogDTO {
    private LocalDateTime timestamp;
    private String action;
    private String ipAddress;
    private String userAgent;
    private String status;
}
