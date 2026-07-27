package com.example.microserviceecom.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PasswordResetMailClient {
    private final RestClient restClient;

    public PasswordResetMailClient(
            @Value("${services.notification.url:http://localhost:8083}") String notificationUrl,
            @Value("${services.notification.internal-api-key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(notificationUrl)
                .defaultHeader("X-Internal-Api-Key", apiKey)
                .build();
    }

    public void send(String email, String otp, long expiresInMinutes) {
        restClient.post()
                .uri("/internal/password-reset-email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PasswordResetMailRequest(email, otp, expiresInMinutes))
                .retrieve()
                .toBodilessEntity();
    }

    private record PasswordResetMailRequest(String email, String otp, long expiresInMinutes) { }
}
