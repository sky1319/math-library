package com.example.library.controller;

import com.example.library.dto.response.ApiResponse;
import com.example.library.service.EbookService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ebooks")
public class AdminEbookController {

    private final EbookService ebookService;

    public AdminEbookController(EbookService ebookService) {
        this.ebookService = ebookService;
    }

    @PostMapping("/{isbn}/unpublish")
    public ApiResponse<Void> unpublish(@PathVariable String isbn) {
        ebookService.unpublish(isbn);
        return ApiResponse.success("电子资源已下架", null);
    }
}
