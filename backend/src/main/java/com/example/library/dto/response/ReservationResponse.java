package com.example.library.dto.response;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        String isbn,
        String bookTitle,
        String status,
        int queuePosition,
        LocalDateTime reservedAt,
        LocalDateTime notifiedAt,
        LocalDateTime expiresAt) {
}
