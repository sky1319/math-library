package com.example.library.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CatalogManagementProposalResponse(
        String token,
        String status,
        String userRole,
        List<CatalogProposalGroupResponse> groups,
        LocalDateTime expiresAt) {
}
