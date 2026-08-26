
package com.example.library.controller;

import com.example.library.dto.request.BookRequest;
import com.example.library.dto.request.PasswordResetRequest;
import com.example.library.dto.request.UserAdminRequest;
import com.example.library.dto.response.ApiResponse;
import com.example.library.dto.response.BookResponse;
import com.example.library.dto.response.BorrowRecordResponse;
import com.example.library.dto.response.CategoryStatsResponse;
import com.example.library.dto.response.UserResponse;
import com.example.library.entity.OperationLog;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.OperationLogService;
import com.example.library.service.StatisticsService;
import com.example.library.service.UserService;
import com.example.library.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.groups.Default;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private BorrowService borrowService;
    
    @Autowired
    private StatisticsService statisticsService;
    
    @Autowired
    private OperationLogService logService;

    @Autowired
    private UserService userService;
    
    @GetMapping("/books")
    public ApiResponse<List<BookResponse>> getAllBooks() {
        return ApiResponse.success(bookService.getAllBooks());
    }
    
    @GetMapping("/books/{isbn}")
    public ApiResponse<BookResponse> getBook(@PathVariable String isbn) {
        BookResponse book = bookService.getBookByIsbn(isbn);
        if (book == null) {
            throw BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在");
        }
        return ApiResponse.success(book);
    }
    
    @PostMapping("/books")
    public ApiResponse<BookResponse> addBook(
            @Validated(BookRequest.Create.class) @RequestBody BookRequest request,
            Authentication auth) {
        BookResponse book = bookService.addBook(request, auth.getName());
        return ApiResponse.success("图书添加成功", book);
    }
    
    @PutMapping("/books/{isbn}")
    public ApiResponse<BookResponse> updateBook(
            @PathVariable String isbn,
            @Valid @RequestBody BookRequest request,
            Authentication auth) {
        BookResponse book = bookService.updateBook(isbn, request, auth.getName());
        if (book == null) {
            throw BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在");
        }
        return ApiResponse.success("图书更新成功", book);
    }
    
    @DeleteMapping("/books/{isbn}")
    public ApiResponse<Void> deleteBook(@PathVariable String isbn, Authentication auth) {
        boolean success = bookService.deleteBook(isbn, auth.getName());
        if (!success) {
            throw BusinessException.conflict("BOOK_DELETE_CONFLICT", "删除失败，图书可能存在在借副本或不存在");
        }
        return ApiResponse.success("图书删除成功", null);
    }
    
    @GetMapping("/borrow-records")
    public ApiResponse<List<BorrowRecordResponse>> getAllBorrowRecords() {
        return ApiResponse.success(borrowService.getUserBorrowRecords(null));
    }
    
    @GetMapping("/borrow-records/user/{userId}")
    public ApiResponse<List<BorrowRecordResponse>> getBorrowRecordsByUser(@PathVariable String userId) {
        return ApiResponse.success(borrowService.getUserBorrowRecords(userId));
    }
    
    @GetMapping("/borrow-records/book/{isbn}")
    public ApiResponse<List<BorrowRecordResponse>> getBorrowRecordsByBook(@PathVariable String isbn) {
        return ApiResponse.success(borrowService.getBookBorrowRecords(isbn));
    }
    
    @GetMapping("/overdue-warnings")
    public ApiResponse<List<BorrowRecordResponse>> getOverdueWarnings() {
        return ApiResponse.success(borrowService.getAllOverdueRecords());
    }
    
    @GetMapping("/statistics/categories")
    public ApiResponse<List<CategoryStatsResponse>> getCategoryStatistics() {
        return ApiResponse.success(statisticsService.getCategoryBorrowStats());
    }
    
    @GetMapping("/logs")
    public ApiResponse<List<OperationLog>> getLogs() {
        return ApiResponse.success(logService.getAllLogs());
    }

    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    @PostMapping("/users")
    public ApiResponse<UserResponse> createUser(
            @Validated({UserAdminRequest.Create.class, Default.class}) @RequestBody UserAdminRequest request,
            Authentication authentication) {
        return ApiResponse.success("用户创建成功", userService.createUser(request, authentication.getName()));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserAdminRequest request,
            Authentication authentication) {
        return ApiResponse.success("用户更新成功", userService.updateUser(userId, request, authentication.getName()));
    }

    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse<Void> resetUserPassword(
            @PathVariable String userId,
            @Valid @RequestBody PasswordResetRequest request,
            Authentication authentication) {
        userService.resetPassword(userId, request.newPassword(), authentication.getName());
        return ApiResponse.success("密码重置成功", null);
    }
}
