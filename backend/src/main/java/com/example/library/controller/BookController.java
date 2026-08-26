
package com.example.library.controller;

import com.example.library.dto.response.ApiResponse;
import com.example.library.dto.response.BookResponse;
import com.example.library.dto.response.PageResponse;
import com.example.library.service.BookService;
import com.example.library.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title", "author", "category", "totalCount", "borrowedCount");
    
    @Autowired
    private BookService bookService;
    
    @GetMapping("/search")
    public ApiResponse<List<BookResponse>> searchBooks(@RequestParam(required = false, defaultValue = "") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ApiResponse.success(bookService.getAllBooks());
        }
        return ApiResponse.success(bookService.searchBooks(keyword));
    }

    @GetMapping
    public ApiResponse<List<BookResponse>> getAllBooks() {
        return ApiResponse.success(bookService.getAllBooks());
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<BookResponse>> getBooksPage(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") @Pattern(regexp = "(?i)asc|desc") String direction) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw BusinessException.badRequest("INVALID_SORT_FIELD", "不支持的排序字段");
        }
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        return ApiResponse.success(PageResponse.from(
                bookService.getBooksPage(keyword, page, size, sortBy, sortDirection)));
    }
    
    @GetMapping("/{isbn}")
    public ApiResponse<BookResponse> getBook(@PathVariable String isbn) {
        BookResponse book = bookService.getBookByIsbn(isbn);
        if (book == null) {
            throw BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在");
        }
        return ApiResponse.success(book);
    }
}
