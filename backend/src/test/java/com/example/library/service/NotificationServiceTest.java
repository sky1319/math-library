package com.example.library.service;

import com.example.library.entity.UserNotification;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserNotificationRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void createsNotificationOnlyOnceForBusinessKey() {
        UserNotificationRepository repository = mock(UserNotificationRepository.class);
        NotificationService service = new NotificationService(
                repository, mock(BorrowRecordRepository.class), mock(BookRepository.class));
        when(repository.save(any(UserNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserNotification created = service.createIfAbsent(
                "reader-1", "DUE_SOON", "即将到期", "请及时归还", "due:1");

        assertThat(created.getRead()).isFalse();
        verify(repository).save(any(UserNotification.class));

        when(repository.existsByUserIdAndBusinessKey("reader-1", "due:1")).thenReturn(true);
        assertThat(service.createIfAbsent(
                "reader-1", "DUE_SOON", "即将到期", "请及时归还", "due:1")).isNull();
        verify(repository, never()).save(null);
    }
}
