package com.example.library.service;

import com.example.library.entity.User;
import com.example.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Optional first-admin bootstrap. It is disabled by default and reads all
 * credentials from the process environment; no default password is shipped.
 */
@Service
@Order(1)
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdminService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.user:}")
    private String userId;

    @Value("${app.bootstrap-admin.password:}")
    private String password;

    @Value("${app.bootstrap-admin.name:Library Administrator}")
    private String name;

    public BootstrapAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userId == null || userId.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "启用 APP_BOOTSTRAP_ADMIN_ENABLED 时，必须同时提供 APP_BOOTSTRAP_ADMIN_USER 和 APP_BOOTSTRAP_ADMIN_PASSWORD");
        }
        if (userRepository.existsById(userId)) {
            return;
        }

        User admin = new User();
        admin.setUserId(userId.trim());
        admin.setPassword(passwordEncoder.encode(password));
        admin.setName(name == null || name.isBlank() ? "Library Administrator" : name.trim());
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        userRepository.save(admin);
    }
}
