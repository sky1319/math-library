package com.example.library.dto.response;

import java.time.LocalDateTime;

public record EbookResourceResponse(
        String isbn,
        String title,
        String author,
        String rightsStatus,
        String sourceName,
        String sourceUrl,
        String licenseName,
        String licenseUrl,
        String jurisdiction,
        int chapterCount,
        int authorDeathYear,
        int firstPublicationYear,
        String rightsEvidence,
        String contentNotice,
        LocalDateTime verifiedAt,
        ReadingProgressResponse progress) {
}
