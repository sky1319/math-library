package com.example.library.repository;

import com.example.library.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    
    List<ChatHistory> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<ChatHistory> findByUserIdAndSessionIdOrderByCreatedAtAsc(String userId, String sessionId);
    
    List<ChatHistory> findTop50ByUserIdOrderByCreatedAtDesc(String userId);
    
    void deleteByUserIdAndSessionId(String userId, String sessionId);
}
