package com.example.profileservice.dto.req;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UpdateMyProfileRequest(
        @NotBlank(message = "First name cannot be blank")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        String lastName,

        String avatarUrl,

        String bio,

        LocalDate birthDate
) {
}