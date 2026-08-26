package com.example.library.dto.response;

public record EbookChapterResponse(
        int chapterNumber,
        String chapterTitle,
        String contentHtml,
        String sourcePageUrl,
        String licenseName,
        String licenseUrl,
        String attribution,
        String modificationNotice) {
}
