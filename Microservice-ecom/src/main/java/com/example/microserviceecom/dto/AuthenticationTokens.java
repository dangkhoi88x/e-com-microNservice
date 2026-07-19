package com.example.microserviceecom.dto;

/** Internal Identity result. Refresh tokens must never be serialized to clients. */
public record AuthenticationTokens(
        String userId,
        String accessToken,
        String refreshToken
) {
}
