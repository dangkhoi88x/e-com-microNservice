package com.example.profileservice.controller;

import com.example.profileservice.dto.req.CreateUserProfileRequest;
import com.example.profileservice.dto.res.ApiResponse;
import com.example.profileservice.dto.res.UserProfileResponse;
import com.example.profileservice.entity.UserProfile;
import com.example.profileservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-profile")
public class UserProfileController {
        private final UserProfileService userProfileService;
    @PostMapping
    ApiResponse<Void> createUserProfile(@RequestBody @Valid CreateUserProfileRequest request) {
         userProfileService.create(request);
          return ApiResponse.<Void>builder()
                  .status(HttpStatus.CREATED.value())
                  .message("Profile created successfully")
                  .build();

    }
    @GetMapping("/me")
    ApiResponse<UserProfileResponse> myInfo(@AuthenticationPrincipal Jwt jwt) {
    var userId= jwt.getSubject();
    var data = userProfileService.myInfo(userId);
        return ApiResponse.<UserProfileResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("My Info successfully")
                .data(data)
                .build();


    }
}
