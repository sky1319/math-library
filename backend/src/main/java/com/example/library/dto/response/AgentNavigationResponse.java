package com.example.library.dto.response;

public record AgentNavigationResponse(
        String type,
        String isbn,
        String bookTitle,
        int chapterNumber,
        int chapterCount,
        String message) {
}
