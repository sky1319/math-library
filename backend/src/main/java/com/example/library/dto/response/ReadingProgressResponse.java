package com.example.library.dto.response;

import java.time.LocalDateTime;

public record ReadingProgressResponse(int chapterNumber, int scrollPercent, LocalDateTime updatedAt) {
}
