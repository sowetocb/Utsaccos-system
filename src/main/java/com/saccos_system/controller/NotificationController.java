package com.saccos_system.controller;

import com.saccos_system.dto.NotificationDTO.NotificationDTO;
import com.saccos_system.dto.SavingsDTO.UnreadCountDTO;
import com.saccos_system.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notifications management")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications")
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            @RequestHeader("Authorization") String token) {
        List<NotificationDTO> notifications = notificationService.getUserNotifications(token);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @RequestHeader("Authorization") String token) {
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(token);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<UnreadCountDTO> getUnreadCount(
            @RequestHeader("Authorization") String token) {
        long count = notificationService.getUnreadCount(token);
        UnreadCountDTO response = new UnreadCountDTO();
        response.setCount(count);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(token, notificationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("Authorization") String token) {
        notificationService.markAllAsRead(token);
        return ResponseEntity.ok().build();
    }
}

