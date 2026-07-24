package com.example.microserviceecom.controller;

import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.dto.response.ApiResponse;
import com.example.microserviceecom.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
public class AdminUserRoleController {
    private final RoleService roleService;

    @PostMapping("/{userId}/roles/seller")
    public ApiResponse<Void> grantSellerRole(@PathVariable String userId) {
        roleService.grantRole(userId, RoleName.SELLER);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("SELLER role granted successfully")
                .build();
    }
}
