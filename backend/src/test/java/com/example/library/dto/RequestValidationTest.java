package com.example.library.dto;

import com.example.library.dto.request.AgentChatRequest;
import com.example.library.dto.request.BookRequest;
import com.example.library.dto.request.LoginRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankLoginCredentials() {
        LoginRequest request = new LoginRequest("", "");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("userId", "password");
    }

    @Test
    void validatesRequiredCreateBookFields() {
        BookRequest request = new BookRequest();
        request.setTotalCount(-1);

        assertThat(validator.validate(request, BookRequest.Create.class))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("isbn", "title", "author", "totalCount");
    }

    @Test
    void rejectsInvalidAgentRequest() {
        AgentChatRequest request = new AgentChatRequest(" ", "short");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("question", "sessionId");
    }
}
