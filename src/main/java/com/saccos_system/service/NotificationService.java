package com.saccos_system.service;

import com.saccos_system.dto.NotificationDTO;
import com.saccos_system.model.*;
import com.saccos_system.repository.NotificationRepository;
import com.saccos_system.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public List<NotificationDTO> getUserNotifications(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));

        List<Notification> notifications = notificationRepository
                .findByUser_UserIdOrderByCreatedDateDesc(userId);

        return notifications.stream()
                .map(this::mapToNotificationDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getUnreadNotifications(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));

        List<Notification> notifications = notificationRepository
                .findByUser_UserIdAndIsReadFalseOrderByCreatedDateDesc(userId);

        return notifications.stream()
                .map(this::mapToNotificationDTO)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    public void markAsRead(String token, Long notificationId) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify notification belongs to user
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notification.setIsRead(true);
        notification.setReadDate(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public void markAllAsRead(String token) {
        Long userId = jwtTokenUtil.getUserIdFromToken(extractToken(token));

        List<Notification> unreadNotifications = notificationRepository
                .findByUser_UserIdAndIsReadFalseOrderByCreatedDateDesc(userId);

        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadDate(LocalDateTime.now());
        });

        notificationRepository.saveAll(unreadNotifications);
    }

    // Notification creation methods
    public void sendDepositNotification(SystemUser user, BigDecimal amount, BigDecimal newBalance) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setProfile(user.getProfile());
        notification.setTitle("Deposit Successful");
        notification.setMessage(String.format(
                "Your deposit of %,.2f has been processed. New balance: %,.2f",
                amount, newBalance));
        notification.setType("SUCCESS");
        notification.setCategory("TRANSACTION");
        notification.setIsImportant(true);

        notificationRepository.save(notification);
        log.info("Deposit notification sent to user: {}", user.getUsername());
    }

    public void sendWithdrawalNotification(SystemUser user, BigDecimal amount, BigDecimal newBalance) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setProfile(user.getProfile());
        notification.setTitle("Withdrawal Successful");
        notification.setMessage(String.format(
                "Your withdrawal of %,.2f has been processed. New balance: %,.2f",
                amount, newBalance));
        notification.setType("INFO");
        notification.setCategory("TRANSACTION");

        notificationRepository.save(notification);
        log.info("Withdrawal notification sent to user: {}", user.getUsername());
    }

    public void sendLoanApplicationNotification(SystemUser user, LoanApplication application) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setProfile(user.getProfile());
        notification.setTitle("Loan Application Submitted");
        notification.setMessage(String.format(
                "Your loan application #%s for %,.2f has been submitted and is under review.",
                application.getApplicationNumber(), application.getAmount()));
        notification.setType("INFO");
        notification.setCategory("LOAN");

        notificationRepository.save(notification);
        log.info("Loan application notification sent to user: {}", user.getUsername());
    }

    public void sendLoanPaymentNotification(SystemUser user, Loan loan, BigDecimal amount) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setProfile(user.getProfile());
        notification.setTitle("Loan Payment Processed");
        notification.setMessage(String.format(
                "Your loan payment of %,.2f for loan #%s has been processed. Remaining balance: %,.2f",
                amount, loan.getLoanNumber(), loan.getRemainingBalance()));
        notification.setType("SUCCESS");
        notification.setCategory("LOAN");

        notificationRepository.save(notification);
        log.info("Loan payment notification sent to user: {}", user.getUsername());
    }

    public void sendStatementGeneratedNotification(SystemUser user, MonthlyStatement statement) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setProfile(user.getProfile());
        notification.setTitle("Monthly Statement Generated");
        notification.setMessage(String.format(
                "Your statement for %s %d has been generated. Statement #%s",
                getMonthName(statement.getMonth()), statement.getYear(), statement.getStatementNumber()));
        notification.setType("INFO");
        notification.setCategory("SAVINGS");
        notification.setActionUrl("/api/statements/" + statement.getStatementNumber() + "/download");

        notificationRepository.save(notification);
        log.info("Statement notification sent to user: {}", user.getUsername());
    }

    public void sendSystemNotification(Long userId, String title, String message,
                                       String type, String category) {
        Notification notification = new Notification();
        notification.setUser(new SystemUser()); // Would fetch actual user
        notification.getUser().setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setCategory(category);
        notification.setIsImportant(true);

        notificationRepository.save(notification);
        log.info("System notification sent to user ID: {}", userId);
    }

    // Helper methods
    private NotificationDTO mapToNotificationDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setNotificationId(notification.getNotificationId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setCategory(notification.getCategory());
        dto.setIsRead(notification.getIsRead());
        dto.setIsImportant(notification.getIsImportant());
        dto.setActionUrl(notification.getActionUrl());
        dto.setCreatedDate(notification.getCreatedDate());
        dto.setReadDate(notification.getReadDate());
        return dto;
    }

    private String getMonthName(int month) {
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return monthNames[month - 1];
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Invalid authorization header");
    }
}