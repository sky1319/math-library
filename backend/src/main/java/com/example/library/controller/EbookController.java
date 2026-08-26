package com.example.library.controller;

import com.example.library.dto.request.ReadingProgressRequest;
import com.example.library.dto.response.ApiResponse;
import com.example.library.dto.response.EbookChapterResponse;
import com.example.library.dto.response.EbookResourceResponse;
import com.example.library.dto.response.ReadingProgressResponse;
import com.example.library.service.EbookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books/{isbn}/ebook")
@Validated
public class EbookController {

    private final EbookService ebookService;

    public EbookController(EbookService ebookService) {
        this.ebookService = ebookService;
    }

    @GetMapping
    public ApiResponse<EbookResourceResponse> getResource(
            @PathVariable String isbn, Authentication authentication) {
        return ApiResponse.success(ebookService.getResource(isbn, authentication.getName()));
    }

    @GetMapping("/chapters/{chapterNumber}")
    public ApiResponse<EbookChapterResponse> getChapter(
            @PathVariable String isbn, @PathVariable @Min(1) int chapterNumber) {
        return ApiResponse.success(ebookService.getChapter(isbn, chapterNumber));
    }

    @PutMapping("/progress")
    public ApiResponse<ReadingProgressResponse> saveProgress(
            @PathVariable String isbn,
            Authentication authentication,
            @Valid @RequestBody ReadingProgressRequest request) {
        return ApiResponse.success("阅读进度已保存",
                ebookService.saveProgress(isbn, authentication.getName(), request));
    }
}
