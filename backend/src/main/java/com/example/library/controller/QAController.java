package com.example.library.controller;

import com.example.library.dto.request.AgentChatRequest;
import com.example.library.dto.response.AgentRunResponse;
import com.example.library.dto.response.AgentActionDraftResponse;
import com.example.library.dto.response.ApiResponse;
import com.example.library.dto.response.BookResponse;
import com.example.library.dto.request.CatalogActionPrepareRequest;
import com.example.library.dto.response.CatalogManagementProposalResponse;
import com.example.library.entity.ChatHistory;
import com.example.library.exception.BusinessException;
import com.example.library.service.AgentAIService;
import com.example.library.service.AgentActionService;
import com.example.library.service.ChatHistoryService;
import com.example.library.service.QAService;
import com.example.library.service.CatalogProposalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/qa")
public class QAController {

    private final QAService qaService;
    private final AgentAIService agentAIService;
    private final ChatHistoryService chatHistoryService;
    private final Executor agentTaskExecutor;
    private final AgentActionService agentActionService;
    private final CatalogProposalService catalogProposalService;

    public QAController(
            QAService qaService,
            AgentAIService agentAIService,
            ChatHistoryService chatHistoryService,
            AgentActionService agentActionService,
            CatalogProposalService catalogProposalService,
            @Qualifier("agentTaskExecutor") Executor agentTaskExecutor) {
        this.qaService = qaService;
        this.agentAIService = agentAIService;
        this.chatHistoryService = chatHistoryService;
        this.agentActionService = agentActionService;
        this.catalogProposalService = catalogProposalService;
        this.agentTaskExecutor = agentTaskExecutor;
    }

    @PostMapping("/actions/{token}/confirm")
    public ApiResponse<AgentActionDraftResponse> confirmAction(
            @PathVariable String token,
            Authentication authentication) {
        return ApiResponse.success(
                "操作执行成功",
                agentActionService.confirm(currentUser(authentication), token));
    }

    @DeleteMapping("/actions/{token}")
    public ApiResponse<Void> cancelAction(
            @PathVariable String token,
            Authentication authentication) {
        agentActionService.cancel(currentUser(authentication), token);
        return ApiResponse.success("操作草案已取消", null);
    }

    @PostMapping("/catalog/proposals/{token}/actions")
    public ApiResponse<AgentActionDraftResponse> prepareCatalogAction(
            @PathVariable String token,
            @Valid @RequestBody CatalogActionPrepareRequest request,
            Authentication authentication) {
        return ApiResponse.success(
                "馆藏操作草案已生成",
                catalogProposalService.prepareAction(currentUser(authentication), token, request));
    }

    @GetMapping
    public ApiResponse<List<BookResponse>> askQuestion(@RequestParam String question) {
        return ApiResponse.success(qaService.searchByQuestion(question));
    }

    @PostMapping("/agent/run")
    public ApiResponse<AgentRunResponse> runAgent(
            @Valid @RequestBody AgentChatRequest request,
            Authentication authentication) throws IOException {
        return ApiResponse.success(agentAIService.runAgent(
                request.question(), currentUser(authentication), request.sessionId()));
    }

    @PostMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgent(
            @Valid @RequestBody AgentChatRequest request,
            Authentication authentication) throws IOException {
        return startAgentStream(request.question(), request.sessionId(), currentUser(authentication));
    }

    @GetMapping("/book/analyze")
    public ApiResponse<String> analyzeBook(
            @RequestParam String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) throws IOException {
        String prompt = "请分析馆藏图书《" + title + "》（作者：" + defaultValue(author, "未知")
                + "，ISBN：" + defaultValue(isbn, "未知") + "），包括馆藏详情、作品背景、阅读建议和同类馆藏推荐。";
        return runAgentForText(prompt, sessionId, authentication);
    }

    @GetMapping("/book/background")
    public ApiResponse<String> getBookBackground(
            @RequestParam String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) throws IOException {
        String prompt = "请结合馆藏信息介绍《" + title + "》（作者：" + defaultValue(author, "未知") + "）的创作背景和作品影响。";
        return runAgentForText(prompt, sessionId, authentication);
    }

    @GetMapping("/book/content")
    public ApiResponse<String> getBookContent(
            @RequestParam String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) throws IOException {
        String prompt = "请结合馆藏信息解析《" + title + "》（作者：" + defaultValue(author, "未知") + "）的内容、人物和主题，避免关键剧透。";
        return runAgentForText(prompt, sessionId, authentication);
    }

    @GetMapping("/book/recommend")
    public ApiResponse<String> getBookRecommendations(
            @RequestParam String title,
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) throws IOException {
        String prompt = "请从真实馆藏中推荐 " + Math.max(1, Math.min(count, 10)) + " 本与《" + title + "》类似的图书，并说明理由和馆藏位置。";
        return runAgentForText(prompt, sessionId, authentication);
    }

    @GetMapping("/recommend")
    public ApiResponse<String> recommendBooks(
            @RequestParam String preferences,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) throws IOException {
        return runAgentForText("请根据我的阅读偏好推荐真实馆藏图书：" + preferences, sessionId, authentication);
    }

    @GetMapping("/chat")
    public ApiResponse<String> chat(
            @RequestParam String question,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) throws IOException {
        return runAgentForText(question, sessionId, authentication);
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter legacyStreamChat(
            @RequestParam String question,
            @RequestParam(required = false) String sessionId,
            Authentication authentication) {
        return startAgentStream(question, sessionId, currentUser(authentication));
    }

    @GetMapping("/history")
    public ApiResponse<List<ChatHistory>> getChatHistory(
            @RequestParam(required = false) String sessionId,
            Authentication authentication) {
        String userId = currentUser(authentication);
        List<ChatHistory> history = sessionId == null || sessionId.isBlank()
                ? chatHistoryService.getUserRecentHistory(userId, 50)
                : chatHistoryService.getSessionHistory(userId, sessionId);
        return ApiResponse.success(history);
    }

    @DeleteMapping("/history")
    public ApiResponse<String> clearChatHistory(
            @RequestParam(required = false) String sessionId,
            Authentication authentication) {
        String userId = currentUser(authentication);
        if (sessionId == null || sessionId.isBlank()) {
            chatHistoryService.clearUserHistory(userId);
        } else {
            chatHistoryService.deleteSession(userId, sessionId);
        }
        return ApiResponse.success("历史记录已清空");
    }

    private ApiResponse<String> runAgentForText(
            String question,
            String sessionId,
            Authentication authentication) throws IOException {
        AgentRunResponse result = agentAIService.runAgent(question, currentUser(authentication), sessionId);
        return ApiResponse.success(result.answer());
    }

    private SseEmitter startAgentStream(String question, String sessionId, String userId) {
        SseEmitter emitter = new SseEmitter(120000L);
        CompletableFuture.runAsync(() -> {
            try {
                sendEvent(emitter, "status", "正在规划查询步骤");
                AgentRunResponse result = agentAIService.runAgent(
                        question,
                        userId,
                        sessionId,
                        status -> sendEvent(emitter, "status", status));
                sendEvent(emitter, "answer", result.answer());
                emitter.send(SseEmitter.event().name("done").data(result));
                emitter.complete();
            } catch (Exception e) {
                try {
                    sendEvent(emitter, "error", safeError(e));
                } catch (RuntimeException ignored) {
                    // Client disconnected before the error event could be sent.
                }
                emitter.completeWithError(e);
            }
        }, agentTaskExecutor);
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            throw new RuntimeException("SSE connection closed", e);
        }
    }

    private String currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw BusinessException.unauthorized("UNAUTHORIZED", "未登录或登录已过期");
        }
        return authentication.getName();
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeError(Exception error) {
        if (error instanceof IllegalArgumentException || error instanceof IOException) {
            return error.getMessage();
        }
        return "AI Agent 暂时不可用，请稍后重试";
    }
}
