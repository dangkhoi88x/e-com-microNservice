package com.example.microserviceecom.service;

import com.example.microserviceecom.client.PasswordResetMailClient;
import com.example.microserviceecom.dto.request.PasswordResetConfirmRequest;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.exception.ErrorCode;
import com.example.microserviceecom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration REQUEST_WINDOW = Duration.ofHours(1);
    private static final int MAX_REQUESTS_PER_HOUR = 5;
    private static final int MAX_REQUESTS_PER_IP_PER_HOUR = 20;
    private static final int MAX_ATTEMPTS = 5;
    private static final DefaultRedisScript<Long> CONSUME_OTP_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "redis.call('DEL', KEYS[1]); redis.call('DEL', KEYS[2]); return 1 else return 0 end",
            Long.class
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PasswordResetMailClient mailClient;
    private final StringRedisTemplate redisTemplate;

    @Value("${security.password-reset.pepper}")
    private String pepper;

    public void request(String rawEmail, String clientAddress) {
        String email = normalize(rawEmail);
        String identity = sha256(email);
        enforceRequestLimits(identity, sha256(clientAddress == null ? "unknown" : clientAddress));

        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String otp = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
            redisTemplate.opsForValue().set(otpKey(identity), otpDigest(email, otp), OTP_TTL);
            redisTemplate.delete(attemptKey(identity));
            try {
                mailClient.send(user.getEmail(), otp, OTP_TTL.toMinutes());
            } catch (RuntimeException exception) {
                redisTemplate.delete(otpKey(identity));
                log.error("Password reset email delivery failed for userId={}", user.getId(), exception);
            }
        });
    }

    @Transactional
    public void confirm(PasswordResetConfirmRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AuthenticationException(ErrorCode.PASSWORD_RESET_PASSWORD_MISMATCH);
        }
        String email = normalize(request.email());
        String identity = sha256(email);
        String suppliedDigest = otpDigest(email, request.otp());
        Long consumed = redisTemplate.execute(
                CONSUME_OTP_SCRIPT,
                List.of(otpKey(identity), attemptKey(identity)),
                suppliedDigest
        );
        if (!Long.valueOf(1).equals(consumed)) {
            registerFailedAttempt(identity);
            throw new AuthenticationException(ErrorCode.PASSWORD_RESET_CODE_INVALID);
        }

        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthenticationException(ErrorCode.PASSWORD_RESET_CODE_INVALID));
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setAuthVersion((user.getAuthVersion() == null ? 0 : user.getAuthVersion()) + 1);
        userRepository.save(user);
        tokenService.revokeAllRefreshSessions(user.getId());
    }

    private void enforceRequestLimits(String identity, String clientIdentity) {
        Boolean cooldownCreated = redisTemplate.opsForValue().setIfAbsent(cooldownKey(identity), "1", RESEND_COOLDOWN);
        if (!Boolean.TRUE.equals(cooldownCreated)) {
            throw new AuthenticationException(ErrorCode.PASSWORD_RESET_REQUEST_LIMITED);
        }
        if (incrementWindow(requestCountKey(identity)) > MAX_REQUESTS_PER_HOUR
                || incrementWindow("password-reset:requests-ip:" + clientIdentity) > MAX_REQUESTS_PER_IP_PER_HOUR) {
            throw new AuthenticationException(ErrorCode.PASSWORD_RESET_REQUEST_LIMITED);
        }
    }

    private long incrementWindow(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) redisTemplate.expire(key, REQUEST_WINDOW);
        return count == null ? 0 : count;
    }

    private void registerFailedAttempt(String identity) {
        Long attempts = redisTemplate.opsForValue().increment(attemptKey(identity));
        if (attempts != null && attempts == 1) redisTemplate.expire(attemptKey(identity), OTP_TTL);
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            redisTemplate.delete(otpKey(identity));
            throw new AuthenticationException(ErrorCode.PASSWORD_RESET_TOO_MANY_ATTEMPTS);
        }
    }

    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private String otpDigest(String email, String otp) { return sha256(pepper + ":" + email + ":" + otp); }
    private String otpKey(String identity) { return "password-reset:otp:" + identity; }
    private String attemptKey(String identity) { return "password-reset:attempts:" + identity; }
    private String cooldownKey(String identity) { return "password-reset:cooldown:" + identity; }
    private String requestCountKey(String identity) { return "password-reset:requests:" + identity; }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
