package com.example.microserviceecom.service;

import com.example.microserviceecom.entity.Token;
import com.example.microserviceecom.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private static final String REFRESH_SESSION_PREFIX = "refresh-session:";
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

    /** Creates a 256-bit opaque refresh token; Redis stores only its SHA-256 hash. */
    public String createRefreshSession(String userId, Duration ttl) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        stringRedisTemplate.opsForValue().set(refreshSessionKey(refreshToken), userId, ttl);
        return refreshToken;
    }

    /** Atomically consume a refresh session so the old token cannot be replayed. */
    public String consumeRefreshSession(String refreshToken) {
        return stringRedisTemplate.opsForValue().getAndDelete(refreshSessionKey(refreshToken));
    }

    public void deleteRefreshSession(String refreshToken) {
        stringRedisTemplate.delete(refreshSessionKey(refreshToken));
    }

    private String refreshSessionKey(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return REFRESH_SESSION_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

}
