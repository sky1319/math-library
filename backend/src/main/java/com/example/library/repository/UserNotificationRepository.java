package com.example.library.repository;

import com.example.library.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserIdAndReadFalse(String userId);
    boolean existsByUserIdAndBusinessKey(String userId, String businessKey);
    Optional<UserNotification> findByIdAndUserId(Long id, String userId);
}
