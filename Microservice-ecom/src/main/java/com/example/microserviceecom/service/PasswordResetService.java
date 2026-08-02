package com.example.microserviceecom.service;

import com.example.microserviceecom.client.PasswordResetMailClient;
import com.example.microserviceecom.dto.request.PasswordResetConfirmRequest;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.exception.ErrorCode;
import com.example.microserviceecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PasswordResetMailClient mailClient;
    private final StringRedisTemplate redisTemplate;

    public void request(String rawEmail) {
        String email = normalize(rawEmail);

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String otp = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
            redisTemplate.opsForValue().set(otpKey(email), otp, OTP_TTL);
            try {
                mailClient.send(user.getEmail(), otp, OTP_TTL.toMinutes());
            } catch (RuntimeException exception) {
                redisTemplate.delete(otpKey(email));
                log.error("Password reset email delivery failed for userId={}", user.getId(), exception);
                throw exception;
            }
        });
    }

    @Transactional
    public void confirm(PasswordResetConfirmRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AuthenticationException(ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH);
        }
        String email = normalize(request.email());
        String savedOtp = redisTemplate.opsForValue().get(otpKey(email));
        if (savedOtp == null || !savedOtp.equals(request.otp())) {
            throw new AuthenticationException(ErrorCode.PASSWORD_RESET_CODE_INVALID);
        }
        redisTemplate.delete(otpKey(email));

        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthenticationException(ErrorCode.PASSWORD_RESET_CODE_INVALID));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setAuthVersion((user.getAuthVersion() == null ? 0 : user.getAuthVersion()) + 1);
        userRepository.save(user);
        tokenService.revokeAllRefreshSessions(user.getId());
    }

    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String otpKey(String email) { return "password-reset:otp:" + email; }
}
