package com.example.profileservice.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record UserProfileResponse(
        String id,
        String userId,
        String firstName,
        String lastName,
        String avatarUrl,
        String bio,
        LocalDate birthDate,
        String phoneNumber,
        String address,
        String city,
        String postalCode
) {

}
