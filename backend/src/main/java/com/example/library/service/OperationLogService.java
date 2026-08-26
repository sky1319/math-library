package com.example.library.service;

import com.example.library.entity.OperationLog;
import com.example.library.repository.OperationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationLogService {
    
    @Autowired
    private OperationLogRepository operationLogRepository;
    
    public void log(String userId, String userRole, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUserRole(userRole);
        log.setOperation(action);
        log.setAction(action);
        log.setDetail(detail);
        log.setTimestamp(LocalDateTime.now());
        
        operationLogRepository.save(log);
    }
    
    public List<OperationLog> getAllLogs() {
        return operationLogRepository.findAllByOrderByTimestampDesc();
    }
}
