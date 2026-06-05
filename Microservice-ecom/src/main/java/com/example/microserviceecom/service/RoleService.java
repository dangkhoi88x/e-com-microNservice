package com.example.microserviceecom.service;


import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.entity.Role;
import com.example.microserviceecom.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public Role createRole(RoleName roleName) {
        return roleRepository.findByNameIgnoreCase(roleName.name())
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(roleName.name())
                        .build()));
    }
}

