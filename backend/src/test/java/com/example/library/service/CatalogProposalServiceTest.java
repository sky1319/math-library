package com.example.library.service;

import com.example.library.entity.Book;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.repository.AgentCatalogProposalRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogProposalServiceTest {

    private AgentCatalogProposalRepository proposalRepository;
    private BookRepository bookRepository;
    private UserRepository userRepository;
    private CatalogProposalService service;

    @BeforeEach
    void setUp() {
        proposalRepository = mock(AgentCatalogProposalRepository.class);
        bookRepository = mock(BookRepository.class);
        userRepository = mock(UserRepository.class);
        service = new CatalogProposalService(
                proposalRepository, bookRepository, userRepository,
                mock(AgentActionService.class), new ObjectMapper());
        ReflectionTestUtils.setField(service, "expirationMinutes", 5);
    }

    @Test
    void adminReceivesRealCandidatesAndDeleteOption() {
        when(userRepository.findById("admin")).thenReturn(Optional.of(user("admin", "ADMIN")));
        Book book = book();
        when(bookRepository.findById("三体")).thenReturn(Optional.empty());
        when(bookRepository.findAll()).thenReturn(List.of(book));
        when(bookRepository.searchBooks("三体")).thenReturn(List.of(book));

        var response = service.propose("admin", List.of("三体"));

        assertThat(response.groups()).hasSize(1);
        assertThat(response.groups().getFirst().candidates().getFirst().isbn()).isEqualTo("isbn-1");
        assertThat(response.groups().getFirst().candidates().getFirst().operations()).contains("DELETE_BOOK");
        verify(proposalRepository).save(any());
    }

    @Test
    void readerCannotCreateCatalogProposal() {
        when(userRepository.findById("reader")).thenReturn(Optional.of(user("reader", "USER")));

        assertThatThrownBy(() -> service.propose("reader", List.of("三体")))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("STAFF_ROLE_REQUIRED"));
    }

    private User user(String id, String role) {
        User user = new User();
        user.setUserId(id);
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    private Book book() {
        Book book = new Book();
        book.setIsbn("isbn-1");
        book.setTitle("三体");
        book.setAuthor("刘慈欣");
        book.setTotalCount(3);
        book.setBorrowedCount(1);
        book.setBorrowable(true);
        return book;
    }
}
