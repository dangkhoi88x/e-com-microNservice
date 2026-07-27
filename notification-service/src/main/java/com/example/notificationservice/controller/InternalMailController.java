package com.example.notificationservice.controller;

import com.example.notificationservice.dto.PasswordResetMailRequest;
import com.example.notificationservice.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalMailController {
    private final MailService mailService;

    @Value("${security.internal-api-key}")
    private String expectedApiKey;

    @PostMapping("/password-reset-email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendPasswordResetEmail(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @RequestBody @Valid PasswordResetMailRequest request
    ) {
        if (!MessageDigest.isEqual(expectedApiKey.getBytes(StandardCharsets.UTF_8), apiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        mailService.sendPasswordResetOtp(request.email(), request.otp(), request.expiresInMinutes());
    }
}
