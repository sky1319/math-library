package com.example.library.dto.response;

public record UserResponse(
        String userId,
        String name,
        String role,
        String email,
        String phone,
        boolean enabled) {
}
