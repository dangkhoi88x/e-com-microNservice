package com.example.profileservice.dto.req;

import jakarta.validation.constraints.NotNull;

public record CreateUserProfileRequest(
        @NotNull
        String userId,
        @NotNull
        String firstName,
        @NotNull
        String lastName
) {
}
