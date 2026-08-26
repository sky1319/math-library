package com.example.library.service;

import com.example.library.entity.ChatHistory;
import com.example.library.repository.ChatHistoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ChatHistoryService {
    
    private final ChatHistoryRepository chatHistoryRepository;

    public ChatHistoryService(ChatHistoryRepository chatHistoryRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
    }
    
    public ChatHistory saveChat(String userId, String question, String response, String responseType) {
        return saveChat(userId, null, question, response, responseType, null, null);
    }
    
    public ChatHistory saveChat(String userId, String question, String response, String responseType, 
                                 String bookTitle, String bookAuthor) {
        return saveChat(userId, null, question, response, responseType, bookTitle, bookAuthor);
    }
    
    public ChatHistory saveChat(String userId, String sessionId, String question, String response, 
                                 String responseType, String bookTitle, String bookAuthor) {
        ChatHistory chat = new ChatHistory();
        chat.setUserId(userId);
        chat.setSessionId(sessionId != null ? sessionId : UUID.randomUUID().toString());
        chat.setUserQuestion(question);
        chat.setAiResponse(response);
        chat.setResponseType(responseType);
        chat.setRelatedBookTitle(bookTitle);
        chat.setRelatedBookAuthor(bookAuthor);
        
        return chatHistoryRepository.save(chat);
    }
    
    public List<ChatHistory> getUserChatHistory(String userId) {
        return chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<ChatHistory> getUserRecentHistory(String userId, int limit) {
        if (limit <= 0 || limit > 100) {
            limit = 50;
        }
        return chatHistoryRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<ChatHistory> getSessionHistory(String userId, String sessionId) {
        return chatHistoryRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(userId, sessionId);
    }

    public List<ChatHistory> getRecentSessionHistory(String userId, String sessionId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<ChatHistory> history = getSessionHistory(userId, sessionId);
        int fromIndex = Math.max(0, history.size() - Math.min(limit, 20));
        return history.subList(fromIndex, history.size());
    }
    
    public void deleteSession(String userId, String sessionId) {
        chatHistoryRepository.deleteByUserIdAndSessionId(userId, sessionId);
    }
    
    public void clearUserHistory(String userId) {
        List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        chatHistoryRepository.deleteAll(history);
    }
}
