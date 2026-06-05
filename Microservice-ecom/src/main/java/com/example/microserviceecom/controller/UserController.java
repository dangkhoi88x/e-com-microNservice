package com.example.microserviceecom.controller;

import com.example.microserviceecom.dto.request.CreateUserRequest;
import com.example.microserviceecom.dto.response.ApiResponse;
import com.example.microserviceecom.dto.response.CreateUserResponse;
import com.example.microserviceecom.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        var data = userService.createUser(request);
        return ApiResponse.<CreateUserResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("User created")
                .data(data)
                .build();
    }

}
