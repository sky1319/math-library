package com.example.library.repository;

import com.example.library.entity.AgentCatalogProposal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AgentCatalogProposalRepository extends JpaRepository<AgentCatalogProposal, Long> {

    Optional<AgentCatalogProposal> findFirstByUserIdAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String userId, String status, LocalDateTime createdAfter);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM AgentCatalogProposal p WHERE p.token = :token AND p.userId = :userId")
    Optional<AgentCatalogProposal> findByTokenAndUserIdForUpdate(
            @Param("token") String token, @Param("userId") String userId);
}
