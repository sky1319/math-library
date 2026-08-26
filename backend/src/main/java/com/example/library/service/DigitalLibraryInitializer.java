package com.example.library.service;

import com.example.library.entity.Book;
import com.example.library.entity.EbookResource;
import com.example.library.repository.BookRepository;
import com.example.library.repository.EbookResourceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(100)
public class DigitalLibraryInitializer implements ApplicationRunner {

    private static final String LICENSE_NAME = "CC BY-SA 4.0";
    private static final String LICENSE_URL = "https://creativecommons.org/licenses/by-sa/4.0/deed.zh-hans";
    private static final String JURISDICTION = "全球公版古典原作；数字文本按 CC BY-SA 4.0 使用";
    private static final String NOTICE = "仅展示古典原文；不包含现代出版社的校注、译文、插图、封面或版式。";
    private static final String EVIDENCE = "作者逝世已超过100年且作品在1930年前发表；中文维基文库来源页标示古典原作为全球公有领域，数字文本贡献按 CC BY-SA 4.0 提供。";

    private static final List<SeedResource> RESOURCES = List.of(
            new SeedResource("红楼梦", "https://zh.wikisource.org/wiki/紅樓夢",
                    "紅樓夢/第%03d回", 120, 1763, 1791),
            new SeedResource("三国演义", "https://zh.wikisource.org/wiki/三國演義",
                    "三國演義/第%03d回", 120, 1400, 1522),
            new SeedResource("水浒传", "https://zh.wikisource.org/wiki/水滸傳_(120回本)",
                    "水滸傳_(120回本)/第%03d回", 120, 1372, 1614),
            new SeedResource("西游记", "https://zh.wikisource.org/wiki/西遊記",
                    "西遊記/第%03d回", 100, 1582, 1592));

    private final BookRepository bookRepository;
    private final EbookResourceRepository resourceRepository;

    public DigitalLibraryInitializer(
            BookRepository bookRepository,
            EbookResourceRepository resourceRepository) {
        this.bookRepository = bookRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (SeedResource seed : RESOURCES) {
            bookRepository.findFirstByTitle(seed.title()).ifPresent(book -> createIfAbsent(book, seed));
        }
    }

    private void createIfAbsent(Book book, SeedResource seed) {
        if (resourceRepository.findByIsbn(book.getIsbn()).isPresent()) {
            return;
        }
        EbookResource resource = new EbookResource();
        resource.setIsbn(book.getIsbn());
        resource.setRightsStatus("PUBLIC_DOMAIN_VERIFIED");
        resource.setSourceName("中文维基文库");
        resource.setSourceUrl(seed.sourceUrl());
        resource.setSourcePagePattern(seed.pagePattern());
        resource.setLicenseName(LICENSE_NAME);
        resource.setLicenseUrl(LICENSE_URL);
        resource.setJurisdiction(JURISDICTION);
        resource.setChapterCount(seed.chapterCount());
        resource.setAuthorDeathYear(seed.authorDeathYear());
        resource.setFirstPublicationYear(seed.firstPublicationYear());
        resource.setRightsEvidence(EVIDENCE);
        resource.setContentNotice(NOTICE);
        resource.setVerifiedAt(LocalDateTime.now());
        resource.setPublished(true);
        resourceRepository.save(resource);
    }

    private record SeedResource(
            String title,
            String sourceUrl,
            String pagePattern,
            int chapterCount,
            int authorDeathYear,
            int firstPublicationYear) {
    }
}
