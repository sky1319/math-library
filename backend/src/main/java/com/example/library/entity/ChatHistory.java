package com.example.library.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_history")
public class ChatHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "user_question", nullable = false, length = 2000)
    private String userQuestion;
    
    @Column(name = "ai_response", length = 5000)
    private String aiResponse;
    
    @Column(name = "response_type")
    private String responseType; // chat, book_analyze, book_background, book_content, book_recommend, recommend
    
    @Column(name = "related_book_title")
    private String relatedBookTitle;
    
    @Column(name = "related_book_author")
    private String relatedBookAuthor;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserQuestion() {
        return userQuestion;
    }
    
    public void setUserQuestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }
    
    public String getAiResponse() {
        return aiResponse;
    }
    
    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }
    
    public String getResponseType() {
        return responseType;
    }
    
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
    
    public String getRelatedBookTitle() {
        return relatedBookTitle;
    }
    
    public void setRelatedBookTitle(String relatedBookTitle) {
        this.relatedBookTitle = relatedBookTitle;
    }
    
    public String getRelatedBookAuthor() {
        return relatedBookAuthor;
    }
    
    public void setRelatedBookAuthor(String relatedBookAuthor) {
        this.relatedBookAuthor = relatedBookAuthor;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
