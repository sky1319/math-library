package com.example.library.dto.response;

import java.util.List;

public record CatalogCandidateResponse(
        String isbn,
        String title,
        String author,
        String publisher,
        String category,
        int totalCount,
        int borrowedCount,
        String location,
        boolean borrowable,
        List<String> operations) {
}
