package com.example.library.service;

import com.example.library.entity.Book;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BookReservationRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.BorrowRelationRepository;
import com.example.library.repository.WishListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookServiceTest {

    private BookRepository bookRepository;
    private BookReservationRepository reservationRepository;
    private BookService service;

    @BeforeEach
    void setUp() {
        service = new BookService();
        bookRepository = mock(BookRepository.class);
        reservationRepository = mock(BookReservationRepository.class);
        ReflectionTestUtils.setField(service, "bookRepository", bookRepository);
        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "borrowRecordRepository", mock(BorrowRecordRepository.class));
        ReflectionTestUtils.setField(service, "wishListRepository", mock(WishListRepository.class));
        ReflectionTestUtils.setField(service, "borrowRelationRepository", mock(BorrowRelationRepository.class));
        ReflectionTestUtils.setField(service, "cacheService", mock(DataCacheService.class));
        ReflectionTestUtils.setField(service, "bookKnowledgeService", mock(BookKnowledgeService.class));
        ReflectionTestUtils.setField(service, "logService", mock(OperationLogService.class));
    }

    @Test
    void cannotReduceStockBelowBorrowedAndReservedCopies() {
        Book book = new Book();
        book.setIsbn("isbn-1");
        book.setTitle("测试图书");
        book.setTotalCount(5);
        book.setBorrowedCount(3);
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book));
        when(reservationRepository.countByIsbnAndStatus("isbn-1", "NOTIFIED")).thenReturn(1L);

        assertThatThrownBy(() -> service.reduceStock("isbn-1", 2, "admin"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("BOOK_STOCK_IN_USE"));
        assertThat(book.getTotalCount()).isEqualTo(5);
    }
}
