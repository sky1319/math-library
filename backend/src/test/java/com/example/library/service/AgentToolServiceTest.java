package com.example.library.service;

import com.example.library.entity.Book;
import com.example.library.dto.response.AgentNavigationResponse;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BookRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentToolServiceTest {

    private BookRepository bookRepository;
    private BookKnowledgeService bookKnowledgeService;
    private BorrowService borrowService;
    private WishListService wishListService;
    private ObjectMapper objectMapper;
    private EbookService ebookService;
    private AgentToolService service;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        bookKnowledgeService = mock(BookKnowledgeService.class);
        borrowService = mock(BorrowService.class);
        wishListService = mock(WishListService.class);
        objectMapper = new ObjectMapper();
        ebookService = mock(EbookService.class);
        service = new AgentToolService(
                bookRepository, bookKnowledgeService, borrowService, wishListService, objectMapper,
                null, null, null, null, ebookService);
    }

    @Test
    void rejectsArgumentsOutsideTheDeclaredSchema() throws Exception {
        String output = service.execute(
                "search_catalog",
                "{\"query\":\"科幻\",\"unexpected\":true}",
                "student001");

        JsonNode result = objectMapper.readTree(output);
        assertThat(result.get("ok").asBoolean()).isFalse();
        assertThat(result.get("error").asText()).contains("未定义参数");
        verifyNoInteractions(bookKnowledgeService);
    }

    @Test
    void returnsStructuredGroundedCatalogResults() throws Exception {
        Book book = new Book();
        book.setIsbn("9780000000001");
        book.setTitle("测试科幻书");
        book.setAuthor("测试作者");
        book.setCategory("科幻小说");
        book.setLocation("A区-01-001");
        book.setTotalCount(3);
        book.setBorrowedCount(1);
        book.setBorrowable(true);
        when(bookKnowledgeService.searchRelevantBooks("科幻", 3)).thenReturn(List.of(book));

        String output = service.execute(
                "search_catalog",
                "{\"query\":\"科幻\",\"limit\":3}",
                "student001");

        JsonNode result = objectMapper.readTree(output);
        assertThat(result.get("ok").asBoolean()).isTrue();
        assertThat(result.at("/data/books/0/title").asText()).isEqualTo("测试科幻书");
        assertThat(result.at("/data/books/0/availableCount").asInt()).isEqualTo(2);
    }

    @Test
    void personalToolsAreBoundToTheAuthenticatedUser() throws Exception {
        when(borrowService.getUserBorrowRecords("student001")).thenReturn(List.of());

        String output = service.execute("get_my_borrowing", "{}", "student001");

        assertThat(objectMapper.readTree(output).get("ok").asBoolean()).isTrue();
        verify(borrowService).getUserBorrowRecords("student001");
    }

    @Test
    void opensOnlyNavigationReturnedByTheVerifiedEbookService() throws Exception {
        when(ebookService.prepareReaderNavigation("红楼梦", 10, "student001"))
                .thenReturn(new AgentNavigationResponse(
                        "OPEN_EBOOK", "isbn-1", "红楼梦", 10, 120, "已通过权属核验"));

        String output = service.execute(
                "open_verified_ebook",
                "{\"isbn_or_title\":\"红楼梦\",\"chapter\":10}",
                "student001");

        JsonNode result = objectMapper.readTree(output);
        assertThat(result.path("ok").asBoolean()).isTrue();
        assertThat(result.at("/data/type").asText()).isEqualTo("OPEN_EBOOK");
        assertThat(result.at("/data/chapterNumber").asInt()).isEqualTo(10);
        verify(ebookService).prepareReaderNavigation("红楼梦", 10, "student001");
    }

    @Test
    void returnsNoNavigationWhenCopyrightGateRejectsTheBook() throws Exception {
        when(ebookService.prepareReaderNavigation("三体", null, "student001"))
                .thenThrow(BusinessException.notFound("EBOOK_NOT_AVAILABLE", "没有通过权属核验的正文"));

        String output = service.execute(
                "open_verified_ebook",
                "{\"isbn_or_title\":\"三体\"}",
                "student001");

        JsonNode result = objectMapper.readTree(output);
        assertThat(result.path("ok").asBoolean()).isFalse();
        assertThat(result.path("error").asText()).contains("权属核验");
    }
}
