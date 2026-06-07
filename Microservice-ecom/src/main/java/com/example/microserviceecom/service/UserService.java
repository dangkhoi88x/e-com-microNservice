package com.example.microserviceecom.service;

import com.example.microserviceecom.client.UserProfileClient;
import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.dto.internal.CreateUserProfileRequest;
import com.example.microserviceecom.dto.request.CreateUserRequest;
import com.example.microserviceecom.dto.response.ApiResponse;
import com.example.microserviceecom.dto.response.CreateUserResponse;
import com.example.microserviceecom.entity.Role;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.exception.ErrorCode;
import com.example.microserviceecom.mapper.UserMapper;
import com.example.microserviceecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final UserProfileClient userProfileClient;

@Transactional(rollbackFor = AuthenticationException.class)
    public CreateUserResponse createUser(CreateUserRequest request) {
        String email = request.getEmail();
        if(userRepository.existsByEmail(email)) {
            throw new AuthenticationException(ErrorCode.USER_EXISTED);
        }
        User user = UserMapper.INSTANCE.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        Role role = roleService.createRole(RoleName.USER);
        user.addRole(role);
        userRepository.save(user);
    ApiResponse<Void> userProfile = userProfileClient.createUserProfile(CreateUserProfileRequest.builder()
            .userId(user.getId())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .build());
    if(userProfile.getStatus() == 201 ){


    }
    return UserMapper.INSTANCE.toCreateUserResponse(user);
    }



}
