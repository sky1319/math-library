package com.example.library.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_logs")
public class OperationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private LocalDateTime timestamp;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "user_role")
    private String userRole;
    
    private String operation;
    
    private String action;
    
    @Column(length = 2000)
    private String detail;

    public OperationLog() {}

    public OperationLog(Long id, LocalDateTime timestamp, String userId, String userRole, String operation, String action, String detail) {
        this.id = id;
        this.timestamp = timestamp;
        this.userId = userId;
        this.userRole = userRole;
        this.operation = operation;
        this.action = action;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
