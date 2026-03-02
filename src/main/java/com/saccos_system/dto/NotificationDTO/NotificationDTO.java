package com.saccos_system.dto.NotificationDTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long notificationId;
    private String title;
    private String message;
    private String type; // INFO, WARNING, SUCCESS, ERROR
    private String category; // SYSTEM, TRANSACTION, LOAN, SAVINGS
    private Boolean isRead;
    private Boolean isImportant;
    private String actionUrl;
    private LocalDateTime createdDate;  // This is the date field
    private LocalDateTime readDate;

    // You can add a helper method to get formatted date if needed
    public String getFormattedDate() {
        if (createdDate != null) {
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            return createdDate.format(formatter);
        }
        return "";
    }
}