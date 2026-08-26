
package com.example.library.controller;

import com.example.library.dto.response.ApiResponse;
import com.example.library.dto.response.BookResponse;
import com.example.library.dto.response.BorrowRecordResponse;
import com.example.library.dto.response.NotificationResponse;
import com.example.library.dto.response.ReservationResponse;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.WishListService;
import com.example.library.service.NotificationService;
import com.example.library.service.ReservationService;
import com.example.library.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private BorrowService borrowService;
    
    @Autowired
    private WishListService wishListService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private NotificationService notificationService;
    
    @GetMapping("/books/search")
    public ApiResponse<List<BookResponse>> searchBooks(@RequestParam String keyword) {
        return ApiResponse.success(bookService.searchBooks(keyword));
    }
    
    @GetMapping("/books/{isbn}")
    public ApiResponse<BookResponse> getBook(@PathVariable String isbn) {
        BookResponse book = bookService.getBookByIsbn(isbn);
        if (book == null) {
            throw BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在");
        }
        return ApiResponse.success(book);
    }
    
    @GetMapping("/books/{isbn}/similar")
    public ApiResponse<List<BookResponse>> getSimilarBooks(@PathVariable String isbn) {
        return ApiResponse.success(bookService.findSimilarBooks(isbn));
    }
    
    @PostMapping("/borrow/{isbn}")
    public ApiResponse<Void> borrowBook(@PathVariable String isbn, Authentication auth) {
        borrowService.borrowBook(auth.getName(), isbn);
        return ApiResponse.success("借阅成功", null);
    }
    
    @PostMapping("/return/{isbn}")
    public ApiResponse<Void> returnBook(@PathVariable String isbn, Authentication auth) {
        borrowService.returnBook(auth.getName(), isbn);
        return ApiResponse.success("归还成功", null);
    }

    @PostMapping("/renew/{isbn}")
    public ApiResponse<BorrowRecordResponse> renewBook(@PathVariable String isbn, Authentication auth) {
        return ApiResponse.success("续借成功", borrowService.renewBook(auth.getName(), isbn));
    }
    
    @GetMapping("/borrow-records")
    public ApiResponse<List<BorrowRecordResponse>> getMyBorrowRecords(Authentication auth) {
        return ApiResponse.success(borrowService.getUserBorrowRecords(auth.getName()));
    }
    
    @GetMapping("/wish-list")
    public ApiResponse<List<BookResponse>> getWishList(Authentication auth) {
        return ApiResponse.success(wishListService.getUserWishList(auth.getName()));
    }
    
    @PostMapping("/wish-list/{isbn}")
    public ApiResponse<Void> addToWishList(@PathVariable String isbn, Authentication auth) {
        wishListService.addToWishList(auth.getName(), isbn);
        return ApiResponse.success("已添加到愿望单", null);
    }
    
    @DeleteMapping("/wish-list/{isbn}")
    public ApiResponse<Void> removeFromWishList(@PathVariable String isbn, Authentication auth) {
        wishListService.removeFromWishList(auth.getName(), isbn);
        return ApiResponse.success("已从愿望单移除", null);
    }

    @GetMapping("/reservations")
    public ApiResponse<List<ReservationResponse>> getReservations(Authentication auth) {
        return ApiResponse.success(reservationService.getUserReservations(auth.getName()));
    }

    @PostMapping("/reservations/{isbn}")
    public ApiResponse<ReservationResponse> createReservation(@PathVariable String isbn, Authentication auth) {
        return ApiResponse.success("预约成功", reservationService.reserve(auth.getName(), isbn));
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ApiResponse<Void> cancelReservation(@PathVariable Long reservationId, Authentication auth) {
        reservationService.cancel(auth.getName(), reservationId);
        return ApiResponse.success("预约已取消", null);
    }

    @GetMapping("/notifications")
    public ApiResponse<List<NotificationResponse>> getNotifications(Authentication auth) {
        return ApiResponse.success(notificationService.getUserNotifications(auth.getName()));
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<Long> getUnreadNotificationCount(Authentication auth) {
        return ApiResponse.success(notificationService.getUnreadCount(auth.getName()));
    }

    @PutMapping("/notifications/{notificationId}/read")
    public ApiResponse<Void> markNotificationRead(@PathVariable Long notificationId, Authentication auth) {
        notificationService.markRead(auth.getName(), notificationId);
        return ApiResponse.success("通知已读", null);
    }

    @PutMapping("/notifications/read-all")
    public ApiResponse<Void> markAllNotificationsRead(Authentication auth) {
        notificationService.markAllRead(auth.getName());
        return ApiResponse.success("全部通知已读", null);
    }
}
