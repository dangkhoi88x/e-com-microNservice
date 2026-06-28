package com.example.microserviceecom.configuration;

import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.entity.Role;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.repository.UserRepository;
import com.example.microserviceecom.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "APP-INITIAL-DATA")
public class AppInitialDataConfiguration implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@khoimicro.com";
    private static final String ADMIN_PASSWORD = "12345678";

    private static final String SELLER_EMAIL = "seller@khoimicro.com";
    private static final String SELLER_PASSWORD = "12345678";

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String @NonNull ... args) {
        createUserIfNotExists(ADMIN_EMAIL, ADMIN_PASSWORD, RoleName.ADMIN);
        createUserIfNotExists(SELLER_EMAIL, SELLER_PASSWORD, RoleName.SELLER);

        log.info("Initial users created successfully");
    }

    private void createUserIfNotExists(String email, String password, RoleName roleName) {
        if (userRepository.existsByEmail(email)) {
            log.info("Initial user already exists: {}", email);
            return;
        }

        Role role = roleService.createRole(roleName);
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();
        user.addRole(role);

        userRepository.save(user);
        log.info("Initial user created: email={}, role={}", email, roleName.name());
    }
}
