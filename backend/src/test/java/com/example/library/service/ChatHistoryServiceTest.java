package com.example.library.service;

import com.example.library.entity.ChatHistory;
import com.example.library.repository.ChatHistoryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatHistoryServiceTest {

    @Test
    void preservesTheProvidedSessionId() {
        ChatHistoryRepository repository = mock(ChatHistoryRepository.class);
        when(repository.save(any(ChatHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ChatHistoryService service = new ChatHistoryService(repository);

        ChatHistory saved = service.saveChat(
                "student001", "session_123456", "问题", "回答", "agent", null, null);

        assertThat(saved.getUserId()).isEqualTo("student001");
        assertThat(saved.getSessionId()).isEqualTo("session_123456");
        verify(repository).save(saved);
    }

    @Test
    void limitsContextToTheMostRecentSessionMessages() {
        ChatHistoryRepository repository = mock(ChatHistoryRepository.class);
        ChatHistory first = history("第一问");
        ChatHistory second = history("第二问");
        ChatHistory third = history("第三问");
        when(repository.findByUserIdAndSessionIdOrderByCreatedAtAsc("student001", "session_123456"))
                .thenReturn(List.of(first, second, third));
        ChatHistoryService service = new ChatHistoryService(repository);

        List<ChatHistory> recent = service.getRecentSessionHistory("student001", "session_123456", 2);

        assertThat(recent).containsExactly(second, third);
    }

    private ChatHistory history(String question) {
        ChatHistory history = new ChatHistory();
        history.setUserQuestion(question);
        history.setAiResponse("回答");
        return history;
    }
}
