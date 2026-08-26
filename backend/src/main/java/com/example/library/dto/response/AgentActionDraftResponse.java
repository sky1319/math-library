package com.example.library.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record AgentActionDraftResponse(
        String token,
        String actionType,
        String isbn,
        String bookTitle,
        String summary,
        String status,
        LocalDateTime expiresAt,
        Map<String, Object> details) {
}
