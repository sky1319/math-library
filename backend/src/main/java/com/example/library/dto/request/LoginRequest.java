package com.example.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    
    @NotBlank(message = "用户编号不能为空")
    @Size(max = 64, message = "用户编号不能超过64个字符")
    private String userId;

    @NotBlank(message = "密码不能为空")
    @Size(max = 100, message = "密码不能超过100个字符")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
