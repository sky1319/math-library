package com.example.library.repository;

import com.example.library.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    List<OperationLog> findByUserIdOrderByTimestampDesc(String userId);
    List<OperationLog> findAllByOrderByTimestampDesc();
}
