package com.example.library.repository;

import com.example.library.entity.AgentActionDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;

public interface AgentActionDraftRepository extends JpaRepository<AgentActionDraft, Long> {
    Optional<AgentActionDraft> findFirstByUserIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String userId, String status, LocalDateTime createdAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM AgentActionDraft d WHERE d.token = :token AND d.userId = :userId")
    Optional<AgentActionDraft> findByTokenAndUserIdForUpdate(
            @Param("token") String token, @Param("userId") String userId);
}
