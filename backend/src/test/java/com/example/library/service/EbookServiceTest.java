package com.example.library.service;

import com.example.library.dto.request.ReadingProgressRequest;
import com.example.library.entity.Book;
import com.example.library.entity.EbookResource;
import com.example.library.entity.ReadingProgress;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.EbookResourceRepository;
import com.example.library.repository.ReadingProgressRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EbookServiceTest {

    private EbookResourceRepository resourceRepository;
    private ReadingProgressRepository progressRepository;
    private BookRepository bookRepository;
    private EbookService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(EbookResourceRepository.class);
        progressRepository = mock(ReadingProgressRepository.class);
        bookRepository = mock(BookRepository.class);
        service = new EbookService(resourceRepository, progressRepository, bookRepository,
                new ObjectMapper(), new OkHttpClient());
    }

    @Test
    void returnsOnlyFullyVerifiedResourceMetadata() {
        EbookResource resource = verifiedResource();
        Book book = new Book();
        book.setIsbn("isbn-1");
        book.setTitle("红楼梦");
        book.setAuthor("曹雪芹");
        when(resourceRepository.findByIsbnAndPublishedTrue("isbn-1")).thenReturn(Optional.of(resource));
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book));
        when(progressRepository.findByUserIdAndResourceId("reader-1", 1L)).thenReturn(Optional.empty());

        var response = service.getResource("isbn-1", "reader-1");

        assertThat(response.title()).isEqualTo("红楼梦");
        assertThat(response.rightsStatus()).isEqualTo("PUBLIC_DOMAIN_VERIFIED");
        assertThat(response.progress().chapterNumber()).isEqualTo(1);
    }

    @Test
    void blocksPublishedRecordWhenLicenseEvidenceIsIncomplete() {
        EbookResource resource = verifiedResource();
        resource.setLicenseUrl("https://untrusted.example/license");
        when(resourceRepository.findByIsbnAndPublishedTrue("isbn-1")).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> service.getResource("isbn-1", "reader-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("EBOOK_RIGHTS_NOT_VERIFIED"));
    }

    @Test
    void rejectsChapterOutsideVerifiedRangeBeforeExternalFetch() {
        when(resourceRepository.findByIsbnAndPublishedTrue("isbn-1"))
                .thenReturn(Optional.of(verifiedResource()));

        assertThatThrownBy(() -> service.getChapter("isbn-1", 121))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("INVALID_CHAPTER"));
    }

    @Test
    void savesProgressOnlyWithinVerifiedResource() {
        EbookResource resource = verifiedResource();
        when(resourceRepository.findByIsbnAndPublishedTrue("isbn-1")).thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId("reader-1", 1L)).thenReturn(Optional.empty());
        when(progressRepository.save(any(ReadingProgress.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.saveProgress("isbn-1", "reader-1", new ReadingProgressRequest(12, 64));

        assertThat(response.chapterNumber()).isEqualTo(12);
        assertThat(response.scrollPercent()).isEqualTo(64);
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void agentNavigationResumesTheAuthenticatedUsersProgress() {
        EbookResource resource = verifiedResource();
        Book book = new Book();
        book.setIsbn("isbn-1");
        book.setTitle("红楼梦");
        ReadingProgress progress = new ReadingProgress();
        progress.setChapterNumber(18);
        progress.setScrollPercent(42);
        when(bookRepository.findFirstByTitle("红楼梦")).thenReturn(Optional.of(book));
        when(resourceRepository.findByIsbnAndPublishedTrue("isbn-1")).thenReturn(Optional.of(resource));
        when(progressRepository.findByUserIdAndResourceId("reader-1", 1L)).thenReturn(Optional.of(progress));

        var navigation = service.prepareReaderNavigation("红楼梦", null, "reader-1");

        assertThat(navigation.type()).isEqualTo("OPEN_EBOOK");
        assertThat(navigation.isbn()).isEqualTo("isbn-1");
        assertThat(navigation.chapterNumber()).isEqualTo(18);
    }

    @Test
    void agentNavigationHonorsAValidRequestedChapter() {
        EbookResource resource = verifiedResource();
        Book book = new Book();
        book.setIsbn("isbn-1");
        book.setTitle("红楼梦");
        when(bookRepository.findById("isbn-1")).thenReturn(Optional.of(book));
        when(resourceRepository.findByIsbnAndPublishedTrue("isbn-1")).thenReturn(Optional.of(resource));

        var navigation = service.prepareReaderNavigation("isbn-1", 36, "reader-1");

        assertThat(navigation.chapterNumber()).isEqualTo(36);
        assertThat(navigation.chapterCount()).isEqualTo(120);
    }

    @Test
    void agentNavigationRejectsBooksWithoutVerifiedFullText() {
        Book book = new Book();
        book.setIsbn("modern-isbn");
        book.setTitle("三体");
        when(bookRepository.findFirstByTitle("三体")).thenReturn(Optional.of(book));
        when(resourceRepository.findByIsbnAndPublishedTrue("modern-isbn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.prepareReaderNavigation("三体", 1, "reader-1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("EBOOK_NOT_AVAILABLE"));
    }

    private EbookResource verifiedResource() {
        EbookResource resource = new EbookResource();
        resource.setId(1L);
        resource.setIsbn("isbn-1");
        resource.setRightsStatus("PUBLIC_DOMAIN_VERIFIED");
        resource.setSourceName("中文维基文库");
        resource.setSourceUrl("https://zh.wikisource.org/wiki/紅樓夢");
        resource.setSourcePagePattern("紅樓夢/第%03d回");
        resource.setLicenseName("CC BY-SA 4.0");
        resource.setLicenseUrl("https://creativecommons.org/licenses/by-sa/4.0/deed.zh-hans");
        resource.setJurisdiction("全球公版古典原作；数字文本按 CC BY-SA 4.0 使用");
        resource.setChapterCount(120);
        resource.setAuthorDeathYear(1763);
        resource.setFirstPublicationYear(1791);
        resource.setRightsEvidence("作者逝世超过100年且来源页标示为公有领域");
        resource.setContentNotice("仅展示古典原文，不包含现代校注、译文、插图或封面");
        resource.setPublished(true);
        resource.setVerifiedAt(LocalDateTime.now());
        return resource;
    }
}
