package com.example.library.service;

import com.example.library.entity.AgentActionDraft;
import com.example.library.entity.Book;
import com.example.library.entity.User;
import com.example.library.dto.request.CatalogActionPrepareRequest;
import com.example.library.exception.BusinessException;
import com.example.library.repository.AgentActionDraftRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentActionServiceTest {

    private AgentActionDraftRepository draftRepository;
    private BookRepository bookRepository;
    private ReservationService reservationService;
    private WishListService wishListService;
    private BorrowService borrowService;
    private AgentActionService service;
    private BookService bookService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        draftRepository = mock(AgentActionDraftRepository.class);
        bookRepository = mock(BookRepository.class);
        reservationService = mock(ReservationService.class);
        wishListService = mock(WishListService.class);
        borrowService = mock(BorrowService.class);
        bookService = mock(BookService.class);
        userRepository = mock(UserRepository.class);
        service = new AgentActionService(
                draftRepository, bookRepository, reservationService, wishListService,
                borrowService, mock(OperationLogService.class), bookService, userRepository, new ObjectMapper());
        ReflectionTestUtils.setField(service, "expirationMinutes", 5);
    }

    @Test
    void preparesDraftWithoutExecutingBusinessOperation() {
        Book book = book();
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book));

        var response = service.prepare("reader-1", "ADD_WISHLIST", "isbn-1");

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.summary()).contains("加入愿望单");
        verify(draftRepository).save(org.mockito.ArgumentMatchers.any(AgentActionDraft.class));
        org.mockito.Mockito.verifyNoInteractions(reservationService, borrowService);
    }

    @Test
    void confirmExecutesActionOnlyOnce() {
        AgentActionDraft draft = draft("ADD_WISHLIST", LocalDateTime.now().plusMinutes(5));
        when(draftRepository.findByTokenAndUserIdForUpdate("token-1", "reader-1"))
                .thenReturn(Optional.of(draft));
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book()));

        var response = service.confirm("reader-1", "token-1");

        assertThat(response.status()).isEqualTo("CONFIRMED");
        verify(wishListService).addToWishList("reader-1", "isbn-1");
    }

    @Test
    void expiredDraftCannotExecute() {
        AgentActionDraft draft = draft("RENEW_BORROW", LocalDateTime.now().minusSeconds(1));
        when(draftRepository.findByTokenAndUserIdForUpdate("token-1", "reader-1"))
                .thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.confirm("reader-1", "token-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("AGENT_ACTION_EXPIRED"));
        org.mockito.Mockito.verifyNoInteractions(borrowService);
    }

    @Test
    void librarianCannotPrepareHardDelete() {
        CatalogActionPrepareRequest request = new CatalogActionPrepareRequest();
        request.setActionType("DELETE_BOOK");
        request.setIsbn("isbn-1");

        assertThatThrownBy(() -> service.prepareManagement("staff-1", "LIBRARIAN", request))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("ADMIN_ROLE_REQUIRED"));
        org.mockito.Mockito.verifyNoInteractions(bookService);
    }

    @Test
    void preparesNewBookDraftWithoutChangingCatalog() {
        CatalogActionPrepareRequest request = new CatalogActionPrepareRequest();
        request.setActionType("ADD_BOOK");
        request.setIsbn("9780000000999");
        request.setTitle("Java测试实践");
        request.setAuthor("测试作者");
        request.setQuantity(3);
        when(bookRepository.existsById("9780000000999")).thenReturn(false);

        var response = service.prepareManagement("admin", "ADMIN", request);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.details()).containsEntry("quantity", 3);
        org.mockito.Mockito.verifyNoInteractions(bookService);
    }

    @Test
    void confirmStockIncreaseRechecksCurrentRole() {
        AgentActionDraft draft = draft("INCREASE_STOCK", LocalDateTime.now().plusMinutes(5));
        draft.setPayloadJson("{\"isbn\":\"isbn-1\",\"title\":\"测试图书\",\"quantity\":2}");
        User admin = new User();
        admin.setUserId("reader-1");
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        when(draftRepository.findByTokenAndUserIdForUpdate("token-1", "reader-1"))
                .thenReturn(Optional.of(draft));
        when(userRepository.findById("reader-1")).thenReturn(Optional.of(admin));
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book()));

        service.confirm("reader-1", "token-1");

        verify(bookService).increaseStock("isbn-1", 2, null, "reader-1");
    }

    private AgentActionDraft draft(String action, LocalDateTime expiresAt) {
        AgentActionDraft draft = new AgentActionDraft();
        draft.setToken("token-1");
        draft.setUserId("reader-1");
        draft.setActionType(action);
        draft.setIsbn("isbn-1");
        draft.setStatus("PENDING");
        draft.setSummary("测试操作");
        draft.setCreatedAt(LocalDateTime.now());
        draft.setExpiresAt(expiresAt);
        return draft;
    }

    private Book book() {
        Book book = new Book();
        book.setIsbn("isbn-1");
        book.setTitle("测试图书");
        return book;
    }
}
