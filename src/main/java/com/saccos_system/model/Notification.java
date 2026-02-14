package com.saccos_system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Notification")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Long notificationId;

    @ManyToOne
    @JoinColumn(name = "UserID")
    private SystemUser user;

    @ManyToOne
    @JoinColumn(name = "ProfileID")
    private StaffProfile profile;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @Column(name = "Message", nullable = false, length = 1000)
    private String message;

    @Column(name = "Type", nullable = false, length = 50)
    private String type; // INFO, WARNING, SUCCESS, ERROR

    @Column(name = "Category", nullable = false, length = 50)
    private String category; // SYSTEM, TRANSACTION, LOAN, SAVINGS

    @Column(name = "IsRead")
    private Boolean isRead = false;

    @Column(name = "IsImportant")
    private Boolean isImportant = false;

    @Column(name = "ActionUrl", length = 500)
    private String actionUrl;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "ReadDate")
    private LocalDateTime readDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
    }
}
