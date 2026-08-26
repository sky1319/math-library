package com.example.library.dto.response;

import java.util.List;

public record AgentRunResponse(
        String answer,
        String sessionId,
        List<String> toolsUsed,
        int iterations,
        AgentActionDraftResponse pendingAction,
        CatalogManagementProposalResponse catalogProposal,
        AgentNavigationResponse navigation) {
}
