package com.example.library.service;

import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.BorrowRelationRepository;
import com.example.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BorrowServiceTest {

    private BorrowRecordRepository recordRepository;
    private BookRepository bookRepository;
    private UserRepository userRepository;
    private OperationLogService logService;
    private ReservationService reservationService;
    private BorrowService service;

    @BeforeEach
    void setUp() {
        recordRepository = mock(BorrowRecordRepository.class);
        bookRepository = mock(BookRepository.class);
        userRepository = mock(UserRepository.class);
        BorrowRelationRepository relationRepository = mock(BorrowRelationRepository.class);
        logService = mock(OperationLogService.class);
        reservationService = mock(ReservationService.class);

        service = new BorrowService();
        ReflectionTestUtils.setField(service, "borrowRecordRepository", recordRepository);
        ReflectionTestUtils.setField(service, "bookRepository", bookRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "borrowRelationRepository", relationRepository);
        ReflectionTestUtils.setField(service, "logService", logService);
        ReflectionTestUtils.setField(service, "reservationService", reservationService);
        ReflectionTestUtils.setField(service, "maxBooks", 5);
        ReflectionTestUtils.setField(service, "maxDays", 30);
        ReflectionTestUtils.setField(service, "maxRenewals", 1);
    }

    @Test
    void borrowsWithAtomicInventoryUpdate() {
        Book book = availableBook();
        User user = user();
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book));
        when(userRepository.findByIdForUpdate("student001")).thenReturn(Optional.of(user));
        when(recordRepository.countByUserIdAndStatus("student001", "BORROWED")).thenReturn(1L);
        when(recordRepository.findByUserId("student001")).thenReturn(List.of());
        when(bookRepository.borrowOneIfAvailable("isbn-1")).thenReturn(1);

        service.borrowBook("student001", "isbn-1");

        verify(bookRepository).borrowOneIfAvailable("isbn-1");
        verify(recordRepository).save(any(BorrowRecord.class));
        verify(logService).log("student001", "USER", "借阅图书", "ISBN: isbn-1, 书名: 测试图书");
    }

    @Test
    void rejectsDuplicateActiveBorrow() {
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(availableBook()));
        when(userRepository.findByIdForUpdate("student001")).thenReturn(Optional.of(user()));
        when(recordRepository.existsByUserIdAndIsbnAndStatus("student001", "isbn-1", "BORROWED"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.borrowBook("student001", "isbn-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.getCode())
                                .isEqualTo("BOOK_ALREADY_BORROWED"));

        verify(bookRepository, never()).borrowOneIfAvailable(any());
    }

    @Test
    void rejectsBorrowLimitBeforeChangingInventory() {
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(availableBook()));
        when(userRepository.findByIdForUpdate("student001")).thenReturn(Optional.of(user()));
        when(recordRepository.countByUserIdAndStatus("student001", "BORROWED")).thenReturn(5L);

        assertThatThrownBy(() -> service.borrowBook("student001", "isbn-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.getCode())
                                .isEqualTo("BORROW_LIMIT_REACHED"));

        verify(bookRepository, never()).borrowOneIfAvailable(any());
    }

    @Test
    void reportsConflictWhenAtomicInventoryUpdateFindsNoCopy() {
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(availableBook()));
        when(userRepository.findByIdForUpdate("student001")).thenReturn(Optional.of(user()));
        when(bookRepository.borrowOneIfAvailable("isbn-1")).thenReturn(0);

        assertThatThrownBy(() -> service.borrowBook("student001", "isbn-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.getCode())
                                .isEqualTo("BOOK_OUT_OF_STOCK"));

        verify(recordRepository, never()).save(any());
    }

    @Test
    void returnsBookWithAtomicInventoryUpdate() {
        BorrowRecord record = new BorrowRecord();
        record.setStatus("BORROWED");
        when(userRepository.findByIdForUpdate("student001")).thenReturn(Optional.of(user()));
        when(recordRepository.findFirstByUserIdAndIsbnAndStatusOrderByIdDesc("student001", "isbn-1", "BORROWED"))
                .thenReturn(record);
        when(bookRepository.returnOneIfBorrowed("isbn-1")).thenReturn(1);

        service.returnBook("student001", "isbn-1");

        verify(bookRepository).returnOneIfBorrowed("isbn-1");
        verify(recordRepository).save(record);
        verify(logService).log("student001", "USER", "归还图书", "ISBN: isbn-1");
        verify(reservationService).notifyNextForBook("isbn-1");
    }

    @Test
    void renewsEligibleBorrowAndExtendsDueDate() {
        BorrowRecord record = new BorrowRecord();
        record.setId(10L);
        record.setUserId("student001");
        record.setIsbn("isbn-1");
        record.setStatus("BORROWED");
        record.setBorrowDate(java.time.LocalDate.now().minusDays(5));
        record.setDueDate(java.time.LocalDate.now().plusDays(10));
        record.setRenewCount(0);
        when(userRepository.findByIdForUpdate("student001")).thenReturn(Optional.of(user()));
        when(recordRepository.findFirstByUserIdAndIsbnAndStatusOrderByIdDesc(
                "student001", "isbn-1", "BORROWED")).thenReturn(record);
        when(userRepository.findById("student001")).thenReturn(Optional.of(user()));
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(availableBook()));

        service.renewBook("student001", "isbn-1");

        org.assertj.core.api.Assertions.assertThat(record.getRenewCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(record.getDueDate())
                .isEqualTo(java.time.LocalDate.now().plusDays(40));
        verify(recordRepository).save(record);
    }

    @Test
    void rejectsRenewalWhenAnotherReaderIsWaiting() {
        BorrowRecord record = new BorrowRecord();
        record.setStatus("BORROWED");
        record.setDueDate(java.time.LocalDate.now().plusDays(5));
        record.setRenewCount(0);
        when(userRepository.findByIdForUpdate("student001")).thenReturn(Optional.of(user()));
        when(recordRepository.findFirstByUserIdAndIsbnAndStatusOrderByIdDesc(
                "student001", "isbn-1", "BORROWED")).thenReturn(record);
        when(reservationService.hasActiveReservationByOtherUser("isbn-1", "student001")).thenReturn(true);

        assertThatThrownBy(() -> service.renewBook("student001", "isbn-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> org.assertj.core.api.Assertions.assertThat(error.getCode())
                                .isEqualTo("BOOK_HAS_RESERVATION_QUEUE"));
    }

    private Book availableBook() {
        Book book = new Book();
        book.setIsbn("isbn-1");
        book.setTitle("测试图书");
        book.setTotalCount(2);
        book.setBorrowedCount(0);
        book.setBorrowable(true);
        return book;
    }

    private User user() {
        User user = new User();
        user.setUserId("student001");
        user.setRole("USER");
        return user;
    }
}
