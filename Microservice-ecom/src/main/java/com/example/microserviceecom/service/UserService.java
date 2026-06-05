package com.example.microserviceecom.service;

import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.dto.request.CreateUserRequest;
import com.example.microserviceecom.dto.response.CreateUserResponse;
import com.example.microserviceecom.entity.Role;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.exception.AppException;
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

@Transactional(rollbackFor = AppException.class)
    public CreateUserResponse createUser(CreateUserRequest request) {
        String email = request.getEmail();
        if(userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        User user = UserMapper.INSTANCE.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        Role role = roleService.createRole(RoleName.USER);
        user.addRole(role);
        userRepository.save(user);
        return UserMapper.INSTANCE.toCreateUserResponse(user);
    }



}
