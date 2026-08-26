package com.example.library.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AgentChatRequest(
        @NotBlank(message = "问题不能为空")
        @Size(max = 1000, message = "问题不能超过1000个字符")
        String question,
        @Pattern(regexp = "[A-Za-z0-9_-]{8,64}", message = "会话编号格式不正确")
        String sessionId) {
}
