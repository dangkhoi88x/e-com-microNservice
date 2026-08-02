package com.example.microserviceecom.service;

import com.example.event.UserCreatedEvent;
import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.dto.request.CreateUserRequest;
import com.example.microserviceecom.dto.response.CreateUserResponse;
import com.example.microserviceecom.entity.Role;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.exception.ErrorCode;
import com.example.microserviceecom.mapper.UserMapper;
import com.example.microserviceecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    private final ApplicationEventPublisher eventPublisher;


    @Transactional(rollbackFor = AuthenticationException.class)
    public CreateUserResponse createUser(CreateUserRequest request) {
        return createUserWithRole(request, RoleName.USER);
    }

    @Transactional(rollbackFor = AuthenticationException.class)
    public CreateUserResponse createUserWithRole(CreateUserRequest request, RoleName roleName) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if(userRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthenticationException(ErrorCode.USER_EXISTED);
        }
        User user = UserMapper.INSTANCE.toUser(request);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Operational accounts are still customer identities. Keeping ROLE_USER
        // ensures their profile and shared authenticated endpoints remain available.
        user.addRole(roleService.createRole(RoleName.USER));
        if (roleName != RoleName.USER) {
            user.addRole(roleService.createRole(roleName));
        }
        userRepository.save(user);
        log.info("User created successfully: {}", user.getId());

        // Publish trong transaction; listener chỉ gửi Kafka sau khi database commit.
        UserCreatedEvent userCreatedEvent = UserCreatedEvent.builder()
                .userId(user.getId())
                .email(email)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        eventPublisher.publishEvent(userCreatedEvent);
        var response = UserMapper.INSTANCE.toCreateUserResponse(user);
        response.setFirstName(request.getFirstName());
        response.setLastName(request.getLastName());

        return response;
    }



}
