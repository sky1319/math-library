package com.example.library.service;

import com.example.library.entity.Book;
import com.example.library.entity.BookReservation;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BookReservationRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceTest {

    private BookReservationRepository reservationRepository;
    private BookRepository bookRepository;
    private BorrowRecordRepository borrowRecordRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private OperationLogService logService;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(BookReservationRepository.class);
        bookRepository = mock(BookRepository.class);
        borrowRecordRepository = mock(BorrowRecordRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        logService = mock(OperationLogService.class);
        service = new ReservationService(
                reservationRepository, bookRepository, borrowRecordRepository,
                userRepository, notificationService, logService);
        ReflectionTestUtils.setField(service, "pickupHours", 48);
        User user = new User();
        user.setUserId("reader-1");
        user.setRole("USER");
        when(userRepository.existsById("reader-1")).thenReturn(true);
        when(userRepository.findById("reader-1")).thenReturn(Optional.of(user));
    }

    @Test
    void createsWaitingReservationWhenNoCopyIsAvailable() {
        Book book = book(2, 2);
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book));
        when(reservationRepository.save(any(BookReservation.class))).thenAnswer(invocation -> {
            BookReservation reservation = invocation.getArgument(0);
            reservation.setId(1L);
            return reservation;
        });
        when(reservationRepository.findByIsbnAndStatusInOrderByReservedAtAsc("isbn-1", Set.of("WAITING")))
                .thenAnswer(invocation -> List.of());

        var response = service.reserve("reader-1", "isbn-1");

        assertThat(response.status()).isEqualTo("WAITING");
        verify(reservationRepository).save(any(BookReservation.class));
    }

    @Test
    void rejectsReservationWhenBookCanBeBorrowedDirectly() {
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book(2, 1)));

        assertThatThrownBy(() -> service.reserve("reader-1", "isbn-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("BOOK_AVAILABLE"));
    }

    @Test
    void notifiesFirstReaderWhenCopyReturns() {
        Book book = book(2, 1);
        BookReservation waiting = new BookReservation();
        waiting.setId(7L);
        waiting.setUserId("reader-1");
        waiting.setIsbn("isbn-1");
        waiting.setStatus("WAITING");
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book));
        when(reservationRepository.countByIsbnAndStatus("isbn-1", "NOTIFIED")).thenReturn(0L);
        when(reservationRepository.findByIsbnAndStatusInOrderByReservedAtAsc("isbn-1", Set.of("WAITING")))
                .thenReturn(List.of(waiting));

        service.notifyNextForBook("isbn-1");

        assertThat(waiting.getStatus()).isEqualTo("NOTIFIED");
        assertThat(waiting.getExpiresAt()).isNotNull();
        verify(notificationService).createIfAbsent(
                org.mockito.ArgumentMatchers.eq("reader-1"),
                org.mockito.ArgumentMatchers.eq("RESERVATION_AVAILABLE"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("reservation-available:7"));
    }

    private Book book(int total, int borrowed) {
        Book book = new Book();
        book.setIsbn("isbn-1");
        book.setTitle("测试图书");
        book.setTotalCount(total);
        book.setBorrowedCount(borrowed);
        book.setBorrowable(true);
        return book;
    }
}
