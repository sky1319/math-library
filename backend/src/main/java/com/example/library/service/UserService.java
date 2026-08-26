package com.example.library.service;

import com.example.library.dto.request.LoginRequest;
import com.example.library.dto.response.LoginResponse;
import com.example.library.dto.request.UserAdminRequest;
import com.example.library.dto.response.UserResponse;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.repository.UserRepository;
import com.example.library.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private OperationLogService logService;
    
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUserId(request.getUserId());
        
        if (user == null) {
            return null;
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw BusinessException.unauthorized("ACCOUNT_DISABLED", "账号已被停用，请联系管理员");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return null;
        }
        
        String token = jwtUtil.generateToken(user.getUserId(), user.getRole(), user.getName());
        
        logService.log(user.getUserId(), user.getRole(), "登录", "用户登录成功");
        
        return new LoginResponse(token, user.getUserId(), user.getName(), user.getRole());
    }
    
    public User getUserById(String userId) {
        return userRepository.findById(userId).orElse(null);
    }
    
    public boolean validatePassword(String userId, String password) {
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPassword());
    }
    
    public boolean existsById(String userId) {
        return userRepository.existsById(userId);
    }
    
    public void saveUser(User user) {
        userRepository.save(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse createUser(UserAdminRequest request, String operatorId) {
        if (userRepository.existsById(request.getUserId())) {
            throw BusinessException.conflict("USER_ID_EXISTS", "用户ID已存在");
        }
        User user = new User();
        user.setUserId(request.getUserId().trim());
        user.setName(request.getName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(normalizeRole(request.getRole()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        userRepository.save(user);
        logService.log(operatorId, "ADMIN", "创建用户", "用户ID: " + user.getUserId() + ", 角色: " + user.getRole());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(String userId, UserAdminRequest request, String operatorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "用户不存在"));
        if (request.getName() != null && !request.getName().isBlank()) user.setName(request.getName().trim());
        if (request.getRole() != null) user.setRole(normalizeRole(request.getRole()));
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getEnabled() != null) {
            if (operatorId.equals(userId) && !request.getEnabled()) {
                throw BusinessException.conflict("CANNOT_DISABLE_SELF", "不能停用当前登录账号");
            }
            user.setEnabled(request.getEnabled());
        }
        userRepository.save(user);
        logService.log(operatorId, "ADMIN", "更新用户", "用户ID: " + userId + ", 角色: " + user.getRole());
        return toResponse(user);
    }

    @Transactional
    public void resetPassword(String userId, String newPassword, String operatorId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logService.log(operatorId, "ADMIN", "重置用户密码", "用户ID: " + userId);
    }

    private String normalizeRole(String role) {
        String normalized = role == null || role.isBlank() ? "USER" : role.trim().toUpperCase();
        if (!List.of("USER", "LIBRARIAN", "ADMIN").contains(normalized)) {
            throw BusinessException.badRequest("INVALID_ROLE", "角色必须为USER、LIBRARIAN或ADMIN");
        }
        return normalized;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(), user.getName(), user.getRole(), user.getEmail(), user.getPhone(),
                Boolean.TRUE.equals(user.getEnabled()));
    }
}
