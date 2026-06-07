package com.example.microserviceecom.dto.internal;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateUserProfileRequest(
        @NotNull
        String userId,
        @NotNull
        String firstName,
        @NotNull
        String lastName
) {
}
