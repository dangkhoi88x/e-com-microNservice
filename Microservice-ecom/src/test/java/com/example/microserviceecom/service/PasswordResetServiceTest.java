package com.example.microserviceecom.service;

import com.example.microserviceecom.client.PasswordResetMailClient;
import com.example.microserviceecom.dto.request.PasswordResetConfirmRequest;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokenService;
    @Mock PasswordResetMailClient mailClient;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, passwordEncoder, tokenService, mailClient, redisTemplate);
        ReflectionTestUtils.setField(service, "pepper", "test-pepper");
    }

    @Test
    void requestDoesNotRevealUnknownEmail() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        service.request("MISSING@example.com", "127.0.0.1");

        verify(mailClient, never()).send(anyString(), anyString(), anyLong());
    }

    @Test
    void confirmChangesPasswordAndRevokesSessions() {
        String email = "user@example.com";
        String otp = "123456";
        String identity = sha256(email);
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);
        User user = User.builder().email(email).password("old").authVersion(2).build();
        user.setId("user-1");
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded");

        service.confirm(new PasswordResetConfirmRequest(email, otp, "new-password", "new-password"));

        verify(userRepository).save(user);
        verify(tokenService).revokeAllRefreshSessions("user-1");
        org.junit.jupiter.api.Assertions.assertEquals("encoded", user.getPassword());
        org.junit.jupiter.api.Assertions.assertEquals(3, user.getAuthVersion());
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
