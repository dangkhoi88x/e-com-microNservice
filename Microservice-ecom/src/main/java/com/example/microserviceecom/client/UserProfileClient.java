package com.example.microserviceecom.client;

import com.example.microserviceecom.dto.internal.CreateUserProfileRequest;
import com.example.microserviceecom.dto.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "profile-service", url = "${services.user-profile.url}")
public interface UserProfileClient {

    @PostMapping("/api/v1/user-profile")
    ApiResponse<Void> createUserProfile(@RequestBody @Valid CreateUserProfileRequest request);


}
