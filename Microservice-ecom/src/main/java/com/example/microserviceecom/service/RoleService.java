package com.example.microserviceecom.service;


import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.entity.Role;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.exception.ErrorCode;
import com.example.microserviceecom.repository.RoleRepository;
import com.example.microserviceecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public Role createRole(RoleName roleName) {
        return roleRepository.findByNameIgnoreCase(roleName.name())
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(roleName.name())
                        .build()));
    }

    @Transactional
    public void grantRole(String userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException(ErrorCode.USER_NOT_FOUND));

        boolean alreadyGranted = user.getUserHasRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getName().equalsIgnoreCase(roleName.name()));
        if (!alreadyGranted) {
            user.addRole(createRole(roleName));
            userRepository.save(user);
        }
    }
}
