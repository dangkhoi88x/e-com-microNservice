package com.example.profileservice.controller;

import com.example.profileservice.dto.req.UpdateMyProfileRequest;
import com.example.profileservice.dto.res.ApiResponse;
import com.example.profileservice.dto.res.UserProfileResponse;
import com.example.profileservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-profile")
@PreAuthorize("hasAuthority('ROLE_USER')")
public class UserProfileController {
        private final UserProfileService userProfileService;

    @GetMapping("/me")
    ApiResponse<UserProfileResponse> myInfo(@AuthenticationPrincipal Jwt jwt) {
        var userId= jwt.getSubject();
        var data = userProfileService.myInfo(userId);
        return ApiResponse.<UserProfileResponse>builder()
                .status(HttpStatus.OK.value())
                .message("My Info successfully")
                .data(data)
                .build();


    }
    @PutMapping("/me")
    ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateMyProfileRequest request
    ) {
        var userId = jwt.getSubject();
        var data = userProfileService.updateMyProfile(userId, request);

        return ApiResponse.<UserProfileResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Profile updated successfully")
                .data(data)
                .build();
    }
}
