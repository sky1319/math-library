package com.example.library.service;

import com.example.library.dto.response.ReservationResponse;
import com.example.library.entity.Book;
import com.example.library.entity.BookReservation;
import com.example.library.exception.BusinessException;
import com.example.library.repository.BookRepository;
import com.example.library.repository.BookReservationRepository;
import com.example.library.repository.BorrowRecordRepository;
import com.example.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ReservationService {

    private static final Set<String> ACTIVE_STATUSES = Set.of("WAITING", "NOTIFIED");

    private final BookReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    @Value("${app.reservation.pickup-hours:48}")
    private int pickupHours;

    public ReservationService(
            BookReservationRepository reservationRepository,
            BookRepository bookRepository,
            BorrowRecordRepository borrowRecordRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            OperationLogService operationLogService) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public ReservationResponse reserve(String userId, String isbn) {
        Book book = bookRepository.findById(isbn)
                .orElseThrow(() -> BusinessException.notFound("BOOK_NOT_FOUND", "图书不存在"));
        if (!userRepository.existsById(userId)) {
            throw BusinessException.notFound("USER_NOT_FOUND", "用户不存在");
        }
        if (borrowRecordRepository.existsByUserIdAndIsbnAndStatus(userId, isbn, "BORROWED")) {
            throw BusinessException.conflict("BOOK_ALREADY_BORROWED", "你已经借阅了这本图书");
        }
        if (availableCount(book) > 0) {
            throw BusinessException.conflict("BOOK_AVAILABLE", "该图书当前有库存，请直接借阅");
        }
        if (reservationRepository.existsByUserIdAndIsbnAndStatusIn(userId, isbn, ACTIVE_STATUSES)) {
            throw BusinessException.conflict("RESERVATION_DUPLICATE", "你已经预约了这本图书");
        }

        BookReservation reservation = new BookReservation();
        reservation.setUserId(userId);
        reservation.setIsbn(isbn);
        reservation.setStatus("WAITING");
        reservation.setReservedAt(LocalDateTime.now());
        reservationRepository.save(reservation);
        operationLogService.log(userId, roleOf(userId), "预约图书", "ISBN: " + isbn + ", 书名: " + book.getTitle());
        return toResponse(reservation);
    }

    public List<ReservationResponse> getUserReservations(String userId) {
        return reservationRepository.findByUserIdOrderByReservedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void cancel(String userId, Long reservationId) {
        BookReservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> BusinessException.notFound("RESERVATION_NOT_FOUND", "预约记录不存在"));
        if (!reservation.getUserId().equals(userId)) {
            throw BusinessException.notFound("RESERVATION_NOT_FOUND", "预约记录不存在");
        }
        cancelActive(reservation);
    }

    @Transactional
    public void cancelByBook(String userId, String isbn) {
        BookReservation reservation = reservationRepository
                .findFirstByUserIdAndIsbnAndStatusInOrderByReservedAtDesc(userId, isbn, ACTIVE_STATUSES)
                .orElseThrow(() -> BusinessException.notFound("RESERVATION_NOT_FOUND", "没有找到有效预约"));
        cancelActive(reservation);
    }

    public void assertBorrowAllowed(String userId, String isbn) {
        List<BookReservation> notified = reservationRepository
                .findByIsbnAndStatusInOrderByReservedAtAsc(isbn, Set.of("NOTIFIED"));
        if (!notified.isEmpty() && notified.stream().noneMatch(item -> item.getUserId().equals(userId))) {
            throw BusinessException.conflict("BOOK_RESERVED_FOR_ANOTHER_USER", "该图书已为其他预约用户保留");
        }
    }

    public boolean hasActiveReservationByOtherUser(String isbn, String userId) {
        return reservationRepository.findByIsbnAndStatusInOrderByReservedAtAsc(isbn, ACTIVE_STATUSES).stream()
                .anyMatch(item -> !item.getUserId().equals(userId));
    }

    @Transactional
    public void completeAfterBorrow(String userId, String isbn) {
        reservationRepository.findFirstByUserIdAndIsbnAndStatusInOrderByReservedAtDesc(
                        userId, isbn, ACTIVE_STATUSES)
                .ifPresent(reservation -> {
                    reservation.setStatus("COMPLETED");
                    reservationRepository.save(reservation);
                });
    }

    @Transactional
    public void notifyNextForBook(String isbn) {
        Book book = bookRepository.findById(isbn).orElse(null);
        if (book == null) return;
        int availableSlots = availableCount(book)
                - (int) reservationRepository.countByIsbnAndStatus(isbn, "NOTIFIED");
        if (availableSlots <= 0) return;

        List<BookReservation> waiting = reservationRepository
                .findByIsbnAndStatusInOrderByReservedAtAsc(isbn, Set.of("WAITING"));
        LocalDateTime now = LocalDateTime.now();
        for (BookReservation reservation : waiting.stream().limit(availableSlots).toList()) {
            reservation.setStatus("NOTIFIED");
            reservation.setNotifiedAt(now);
            reservation.setExpiresAt(now.plusHours(Math.max(1, pickupHours)));
            reservationRepository.save(reservation);
            notificationService.createIfAbsent(
                    reservation.getUserId(),
                    "RESERVATION_AVAILABLE",
                    "预约图书已到馆",
                    "你预约的《" + book.getTitle() + "》已可借阅，请在 " + reservation.getExpiresAt() + " 前完成借阅。",
                    "reservation-available:" + reservation.getId());
        }
    }

    @Transactional
    public void expireNotifiedReservations() {
        List<BookReservation> expired = reservationRepository
                .findByStatusAndExpiresAtBefore("NOTIFIED", LocalDateTime.now());
        for (BookReservation reservation : expired) {
            reservation.setStatus("EXPIRED");
            reservationRepository.save(reservation);
            notificationService.createIfAbsent(
                    reservation.getUserId(),
                    "RESERVATION_EXPIRED",
                    "预约保留已过期",
                    "你的图书预约已超过保留时间，系统已将机会顺延给下一位读者。",
                    "reservation-expired:" + reservation.getId());
            notifyNextForBook(reservation.getIsbn());
        }
    }

    private void cancelActive(BookReservation reservation) {
        if (!ACTIVE_STATUSES.contains(reservation.getStatus())) {
            throw BusinessException.conflict("RESERVATION_NOT_ACTIVE", "该预约已经结束，不能取消");
        }
        boolean releasedCopy = "NOTIFIED".equals(reservation.getStatus());
        reservation.setStatus("CANCELLED");
        reservationRepository.save(reservation);
        operationLogService.log(reservation.getUserId(), roleOf(reservation.getUserId()),
                "取消预约", "ISBN: " + reservation.getIsbn());
        if (releasedCopy) notifyNextForBook(reservation.getIsbn());
    }

    private ReservationResponse toResponse(BookReservation reservation) {
        Book book = bookRepository.findById(reservation.getIsbn()).orElse(null);
        int queuePosition = 0;
        if ("WAITING".equals(reservation.getStatus())) {
            List<BookReservation> queue = reservationRepository.findByIsbnAndStatusInOrderByReservedAtAsc(
                    reservation.getIsbn(), Set.of("WAITING"));
            queuePosition = java.util.stream.IntStream.range(0, queue.size())
                    .filter(index -> java.util.Objects.equals(queue.get(index).getId(), reservation.getId()))
                    .map(index -> index + 1)
                    .findFirst()
                    .orElse(0);
        }
        return new ReservationResponse(
                reservation.getId(),
                reservation.getIsbn(),
                book == null ? reservation.getIsbn() : book.getTitle(),
                reservation.getStatus(),
                Math.max(0, queuePosition),
                reservation.getReservedAt(),
                reservation.getNotifiedAt(),
                reservation.getExpiresAt());
    }

    private int availableCount(Book book) {
        int total = book.getTotalCount() == null ? 0 : book.getTotalCount();
        int borrowed = book.getBorrowedCount() == null ? 0 : book.getBorrowedCount();
        return Math.max(0, total - borrowed);
    }

    private String roleOf(String userId) {
        return userRepository.findById(userId).map(user -> user.getRole()).orElse("USER");
    }
}
