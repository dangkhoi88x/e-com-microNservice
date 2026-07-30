package com.example.microserviceecom.service;

import com.example.microserviceecom.entity.Token;
import com.example.microserviceecom.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh-token:";
    private static final String USER_REFRESH_TOKEN_PREFIX = "user-refresh-token:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TokenRepository tokenRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public void saveToken(String jid, String userId, Instant expiration) {
        Instant now = Instant.now();

        long timeToLive = expiration.getEpochSecond() - now.getEpochSecond();

        Token token = Token.builder()
                .tokenId(jid)
                .userId(userId)
                .timeToLive(timeToLive)
                .build();

        tokenRepository.save(token);
    }

    public Token findByJti(String jti) {
        return tokenRepository.findById(jti).orElse(null);
    }

    public void deleteToken(String jti) {
        tokenRepository.deleteById(jti);
    }

    /**
     * Creates one opaque refresh token for a user. A new login replaces the
     * previous refresh session, so this intentionally supports one device at a time.
     */
    public String createRefreshToken(String userId, Duration ttl) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        revokeAllRefreshSessions(userId);
        stringRedisTemplate.opsForValue().set(refreshTokenKey(refreshToken), userId, ttl);
        stringRedisTemplate.opsForValue().set(userRefreshTokenKey(userId), refreshToken, ttl);
        return refreshToken;
    }

    public String findUserIdByRefreshToken(String refreshToken) {
        return refreshToken == null ? null : stringRedisTemplate.opsForValue().get(refreshTokenKey(refreshToken));
    }

    public void deleteRefreshSession(String refreshToken) {
        String userId = findUserIdByRefreshToken(refreshToken);
        if (refreshToken != null) {
            stringRedisTemplate.delete(refreshTokenKey(refreshToken));
        }
        if (userId != null) {
            stringRedisTemplate.delete(userRefreshTokenKey(userId));
        }
    }

    public void revokeAllRefreshSessions(String userId) {
        String userTokenKey = userRefreshTokenKey(userId);
        String refreshToken = stringRedisTemplate.opsForValue().get(userTokenKey);
        if (refreshToken != null) {
            stringRedisTemplate.delete(refreshTokenKey(refreshToken));
        }
        stringRedisTemplate.delete(userTokenKey);
    }

    private String refreshTokenKey(String refreshToken) {
        return REFRESH_TOKEN_PREFIX + refreshToken;
    }

    private String userRefreshTokenKey(String userId) {
        return USER_REFRESH_TOKEN_PREFIX + userId;
    }
}
