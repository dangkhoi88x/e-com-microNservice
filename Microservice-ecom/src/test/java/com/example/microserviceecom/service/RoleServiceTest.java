package com.example.microserviceecom.service;

import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.entity.Role;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.repository.RoleRepository;
import com.example.microserviceecom.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {
    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void grantsSellerRoleOnce() {
        User user = User.builder().email("seller@example.com").build();
        Role sellerRole = Role.builder().name("SELLER").build();
        when(userRepository.findById("seller-1")).thenReturn(Optional.of(user));
        when(roleRepository.findByNameIgnoreCase("SELLER")).thenReturn(Optional.of(sellerRole));

        roleService.grantRole("seller-1", RoleName.SELLER);

        verify(userRepository).save(user);
    }
}
