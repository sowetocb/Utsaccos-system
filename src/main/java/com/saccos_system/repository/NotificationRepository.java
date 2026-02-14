package com.saccos_system.repository;

import com.saccos_system.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_UserIdOrderByCreatedDateDesc(Long userId);
    List<Notification> findByProfile_ProfileIdOrderByCreatedDateDesc(Long profileId);
    List<Notification> findByUser_UserIdAndIsReadFalseOrderByCreatedDateDesc(Long userId);
    List<Notification> findByUser_UserIdAndCategoryOrderByCreatedDateDesc(Long userId, String category);
    long countByUser_UserIdAndIsReadFalse(Long userId);

    @Query("SELECT n FROM Notification n WHERE " +
            "(n.user.userId = :userId OR n.profile.profileId = :profileId) " +
            "AND n.isRead = false " +
            "ORDER BY n.createdDate DESC")
    List<Notification> findUnreadNotifications(@Param("userId") Long userId,
                                               @Param("profileId") Long profileId);
}