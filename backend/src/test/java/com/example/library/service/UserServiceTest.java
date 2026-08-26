package com.example.library.service;

import com.example.library.config.JwtUtil;
import com.example.library.dto.request.LoginRequest;
import com.example.library.dto.request.UserAdminRequest;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository repository;
    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserService();
        ReflectionTestUtils.setField(service, "userRepository", repository);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "jwtUtil", mock(JwtUtil.class));
        ReflectionTestUtils.setField(service, "logService", mock(OperationLogService.class));
    }

    @Test
    void administratorCanCreateLibrarian() {
        UserAdminRequest request = new UserAdminRequest();
        request.setUserId("librarian-1");
        request.setName("测试馆员");
        request.setPassword("password123");
        request.setRole("LIBRARIAN");
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createUser(request, "admin");

        assertThat(response.role()).isEqualTo("LIBRARIAN");
        assertThat(response.enabled()).isTrue();
        verify(repository).save(any(User.class));
    }

    @Test
    void disabledUserCannotLogin() {
        User user = new User();
        user.setUserId("reader-1");
        user.setPassword("encoded");
        user.setEnabled(false);
        when(repository.findByUserId("reader-1")).thenReturn(user);
        LoginRequest request = new LoginRequest();
        request.setUserId("reader-1");
        request.setPassword("password123");

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("ACCOUNT_DISABLED"));
    }
}
