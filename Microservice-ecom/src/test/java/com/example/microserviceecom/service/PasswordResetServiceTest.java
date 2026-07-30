package com.example.microserviceecom.service;

import com.example.microserviceecom.client.PasswordResetMailClient;
import com.example.microserviceecom.dto.request.PasswordResetConfirmRequest;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String OTP_KEY = "password-reset:otp:" + EMAIL;

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
    }

    @Test
    void requestDoesNotRevealUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        service.request(EMAIL);

        verify(mailClient, never()).send(anyString(), anyString(), anyLong());
    }

    @Test
    void requestStoresSixDigitOtpAndSendsItByMail() {
        User user = User.builder().email(EMAIL).build();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);

        service.request(" USER@example.com ");

        verify(valueOperations).set(eq(OTP_KEY), otpCaptor.capture(), eq(Duration.ofMinutes(10)));
        String otp = otpCaptor.getValue();
        assertEquals(6, otp.length());
        verify(mailClient).send(EMAIL, otp, 10);
    }

    @Test
    void confirmChangesPasswordDeletesOtpAndRevokesSessions() {
        String otp = "123456";
        User user = User.builder().email(EMAIL).password("old").authVersion(2).build();
        user.setId("user-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(OTP_KEY)).thenReturn(otp);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded");

        service.confirm(new PasswordResetConfirmRequest(EMAIL, otp, "new-password", "new-password"));

        verify(redisTemplate).delete(OTP_KEY);
        verify(userRepository).save(user);
        verify(tokenService).revokeAllRefreshSessions("user-1");
        assertEquals("encoded", user.getPassword());
        assertEquals(3, user.getAuthVersion());
    }

    @Test
    void confirmRejectsMissingOrExpiredOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(OTP_KEY)).thenReturn(null);

        assertThrows(AuthenticationException.class, () -> service.confirm(
                new PasswordResetConfirmRequest(EMAIL, "123456", "new-password", "new-password")
        ));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(tokenService, never()).revokeAllRefreshSessions(anyString());
    }

    @Test
    void confirmRejectsIncorrectOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(OTP_KEY)).thenReturn("123456");

        assertThrows(AuthenticationException.class, () -> service.confirm(
                new PasswordResetConfirmRequest(EMAIL, "654321", "new-password", "new-password")
        ));

        verify(redisTemplate, never()).delete(OTP_KEY);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
