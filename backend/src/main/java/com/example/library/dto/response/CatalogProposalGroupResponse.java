package com.example.library.dto.response;

import java.util.List;

public record CatalogProposalGroupResponse(
        String query,
        List<CatalogCandidateResponse> candidates,
        boolean canAddNewEdition) {
}
