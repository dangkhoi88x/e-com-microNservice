package com.example.microserviceecom.controller;

import com.example.microserviceecom.common.RoleName;
import com.example.microserviceecom.dto.request.CreateUserRequest;
import com.example.microserviceecom.dto.response.ApiResponse;
import com.example.microserviceecom.dto.response.CreateUserResponse;
import com.example.microserviceecom.service.RoleService;
import com.example.microserviceecom.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserRoleController {
    private final RoleService roleService;
    private final UserService userService;

    @PostMapping("/shippers")
    public ApiResponse<CreateUserResponse> createShipper(@RequestBody @Valid CreateUserRequest request) {
        CreateUserResponse shipper = userService.createUserWithRole(request, RoleName.SHIPPER);
        return ApiResponse.<CreateUserResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("SHIPPER account created successfully")
                .data(shipper)
                .build();
    }

    @PostMapping("/{userId}/roles/seller")
    public ApiResponse<Void> grantSellerRole(@PathVariable String userId) {
        roleService.grantRole(userId, RoleName.SELLER);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("SELLER role granted successfully")
                .build();
    }

    @PostMapping("/{userId}/roles/shipper")
    public ApiResponse<Void> grantShipperRole(@PathVariable String userId) {
        roleService.grantRole(userId, RoleName.SHIPPER);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("SHIPPER role granted successfully")
                .build();
    }
}
