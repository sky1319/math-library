package com.example.library.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReadingProgressRequest(
        @NotNull @Min(1) Integer chapterNumber,
        @NotNull @Min(0) @Max(100) Integer scrollPercent) {
}
