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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Transactional(rollbackFor = AuthenticationException.class)
    public CreateUserResponse createUser(CreateUserRequest request) {
        return createUserWithRole(request, RoleName.USER);
    }

    @Transactional(rollbackFor = AuthenticationException.class)
    public CreateUserResponse createUserWithRole(CreateUserRequest request, RoleName roleName) {
        String email = request.getEmail();
        if(userRepository.existsByEmail(email)) {
            throw new AuthenticationException(ErrorCode.USER_EXISTED);
        }
        User user = UserMapper.INSTANCE.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        Role role = roleService.createRole(roleName);
        user.addRole(role);
        userRepository.save(user);
    log.info("User created successfully: {}", user.getId());
        UserCreatedEvent userCreatedEvent = UserCreatedEvent.builder()
                .userId(user.getId())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        kafkaTemplate.send("created-user-topic", userCreatedEvent).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Error while sending event to topic", throwable);
            }
            log.info("Event sent to topic");

        });
        var response = UserMapper.INSTANCE.toCreateUserResponse(user);
        response.setFirstName(request.getFirstName());
        response.setLastName(request.getLastName());

    return response;
    }



}
