package com.backend.observerr.notification.repository;

import com.backend.observerr.notification.model.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    Page<UserNotification> findByUserId(Long userId, Pageable pageable);
    Page<UserNotification> findByUserIdAndCategory(Long userId, String category, Pageable pageable);
    Page<UserNotification> findByUserIdAndReadAtIsNull(Long userId, Pageable pageable);
    Page<UserNotification> findByUserIdAndCategoryAndReadAtIsNull(
            Long userId, String category, Pageable pageable);
    Optional<UserNotification> findByIdAndUserId(Long id, Long userId);
    Optional<UserNotification> findByUserIdAndDeduplicationKey(Long userId, String deduplicationKey);
    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE UserNotification n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserNotification n WHERE n.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
