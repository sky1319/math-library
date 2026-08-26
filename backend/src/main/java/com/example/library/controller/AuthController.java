
package com.example.library.controller;

import com.example.library.dto.request.LoginRequest;
import com.example.library.dto.response.ApiResponse;
import com.example.library.dto.response.LoginResponse;
import com.example.library.service.BorrowService;
import com.example.library.service.UserService;
import com.example.library.exception.BusinessException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BorrowService borrowService;
    
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("收到登录请求: userId={}", request.getUserId());
        try {
            LoginResponse response = userService.login(request);
            
            if (response == null) {
                logger.warn("登录失败: 用户名或密码错误");
                throw BusinessException.unauthorized("INVALID_CREDENTIALS", "用户名或密码错误");
            }
            
            logger.info("登录成功: userId={}, role={}", response.getUserId(), response.getRole());
            
            if ("ADMIN".equals(response.getRole())) {
                borrowService.checkOverdueAndWarn();
            }
            
            return ApiResponse.success(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("登录时发生错误", e);
            throw new IllegalStateException("登录处理失败", e);
        }
    }
}
