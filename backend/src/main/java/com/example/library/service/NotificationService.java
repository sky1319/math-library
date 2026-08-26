package com.example.library.service;

import com.example.library.dto.response.NotificationResponse;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.entity.UserNotification;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserNotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;

    @Value("${app.notification.due-soon-days:3}")
    private int dueSoonDays;

    public NotificationService(
            UserNotificationRepository notificationRepository,
            BorrowRecordRepository borrowRecordRepository,
            BookRepository bookRepository) {
        this.notificationRepository = notificationRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookRepository = bookRepository;
    }

    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(String userId, Long notificationId) {
        UserNotification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> BusinessException.notFound(
                        "NOTIFICATION_NOT_FOUND", "通知不存在"));
        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllRead(String userId) {
        List<UserNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        notifications.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getRead()))
                .forEach(item -> {
                    item.setRead(true);
                    item.setReadAt(now);
                });
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public UserNotification createIfAbsent(
            String userId,
            String type,
            String title,
            String content,
            String businessKey) {
        if (businessKey != null && notificationRepository.existsByUserIdAndBusinessKey(userId, businessKey)) {
            return null;
        }
        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setBusinessKey(businessKey);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Transactional
    public void generateBorrowingNotifications() {
        LocalDate today = LocalDate.now();
        LocalDate dueSoonEnd = today.plusDays(Math.max(1, dueSoonDays));
        for (BorrowRecord record : borrowRecordRepository.findByDueDateBetweenAndStatus(today, dueSoonEnd, "BORROWED")) {
            Book book = bookRepository.findById(record.getIsbn()).orElse(null);
            String title = book == null ? record.getIsbn() : book.getTitle();
            createIfAbsent(
                    record.getUserId(),
                    "DUE_SOON",
                    "图书即将到期",
                    "《" + title + "》将在 " + record.getDueDate() + " 到期，请及时归还或续借。",
                    "due-soon:" + record.getId() + ":" + record.getDueDate());
        }
        for (BorrowRecord record : borrowRecordRepository.findByDueDateBeforeAndStatus(today, "BORROWED")) {
            Book book = bookRepository.findById(record.getIsbn()).orElse(null);
            String title = book == null ? record.getIsbn() : book.getTitle();
            createIfAbsent(
                    record.getUserId(),
                    "OVERDUE",
                    "图书已经逾期",
                    "《" + title + "》已于 " + record.getDueDate() + " 到期，请尽快归还。",
                    "overdue:" + record.getId() + ":" + today);
        }
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                Boolean.TRUE.equals(notification.getRead()),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
