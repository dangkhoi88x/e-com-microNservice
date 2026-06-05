package com.example.microserviceecom.dto.response;

import lombok.Builder;

@Builder
public record AuthenticationResponse(
        String userId,
        String accessToken,
        String refreshToken
) {
}
