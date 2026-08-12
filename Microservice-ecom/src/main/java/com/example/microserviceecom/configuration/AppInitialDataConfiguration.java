package com.example.microserviceecom.configuration;

import com.example.event.UserCreatedEvent;
import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.entity.Role;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.repository.UserRepository;
import com.example.microserviceecom.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "APP-INITIAL-DATA")
public class AppInitialDataConfiguration implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@khoimicro.com";
    private static final String ADMIN_PASSWORD = "12345678";
    private static final String ADMIN_FIRST_NAME = "Admin";
    private static final String ADMIN_LAST_NAME = "Khoi Micro";

    private static final String SELLER_EMAIL = "seller@khoimicro.com";
    private static final String SELLER_PASSWORD = "12345678";
    private static final String SELLER_FIRST_NAME = "Seller";
    private static final String SELLER_LAST_NAME = "Khoi Micro";

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void run(String @NonNull ... args) {
        createUserIfNotExists(
                ADMIN_EMAIL,
                ADMIN_PASSWORD,
                RoleName.ADMIN,
                ADMIN_FIRST_NAME,
                ADMIN_LAST_NAME
        );
        createUserIfNotExists(
                SELLER_EMAIL,
                SELLER_PASSWORD,
                RoleName.SELLER,
                SELLER_FIRST_NAME,
                SELLER_LAST_NAME
        );

        log.info("Initial users created successfully");
    }

    private void createUserIfNotExists(
            String email,
            String password,
            RoleName roleName,
            String firstName,
            String lastName
    ) {
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            log.info("Initial user already exists: {}", email);
            sendUserCreatedEvent(user, firstName, lastName);
        }, () -> {
            Role role = roleService.createRole(roleName);
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .build();
            user.addRole(role);

            User savedUser = userRepository.save(user);
            sendUserCreatedEvent(savedUser, firstName, lastName);
            log.info("Initial user created: email={}, role={}", email, roleName.name());
        });
    }

    private void sendUserCreatedEvent(User user, String firstName, String lastName) {
        UserCreatedEvent userCreatedEvent = UserCreatedEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .build();

        try {
            kafkaTemplate.send("created-user-topic", userCreatedEvent).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to send initial user event: userId={}", user.getId(), throwable);
                    return;
                }

                log.info("Initial user event sent: userId={}", user.getId());
            });
        } catch (Exception exception) {
            // send() also fails synchronously when the broker is unreachable, and this runs in a
            // CommandLineRunner: letting it escape kills the whole service on startup. Seeding the
            // initial users is not worth blocking authentication on Kafka being up.
            log.warn("Skipped initial user event because Kafka is unavailable: userId={}", user.getId(), exception);
        }
    }
}
