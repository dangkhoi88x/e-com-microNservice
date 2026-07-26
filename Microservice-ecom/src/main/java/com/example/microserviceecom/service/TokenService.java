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
    private static final String USER_REFRESH_SESSIONS_PREFIX = "user-refresh-sessions:";
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
    public String createRefreshSession(String userId, int authVersion, Duration ttl) {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String sessionKey = refreshSessionKey(refreshToken);
        String userSessionsKey = userSessionsKey(userId);
        stringRedisTemplate.opsForValue().set(sessionKey, userId + "|" + authVersion, ttl);
        stringRedisTemplate.opsForSet().add(userSessionsKey, sessionKey);
        stringRedisTemplate.expire(userSessionsKey, ttl);
        return refreshToken;
    }

    /** Atomically consume a refresh session so the old token cannot be replayed. */
    public RefreshSession consumeRefreshSession(String refreshToken) {
        String sessionKey = refreshSessionKey(refreshToken);
        RefreshSession session = parseRefreshSession(stringRedisTemplate.opsForValue().getAndDelete(sessionKey));
        if (session != null) {
            stringRedisTemplate.opsForSet().remove(userSessionsKey(session.userId()), sessionKey);
        }
        return session;
    }

    public void deleteRefreshSession(String refreshToken) {
        String sessionKey = refreshSessionKey(refreshToken);
        RefreshSession session = parseRefreshSession(stringRedisTemplate.opsForValue().get(sessionKey));
        stringRedisTemplate.delete(sessionKey);
        if (session != null) {
            stringRedisTemplate.opsForSet().remove(userSessionsKey(session.userId()), sessionKey);
        }
    }

    public void revokeAllRefreshSessions(String userId) {
        String indexKey = userSessionsKey(userId);
        var sessionKeys = stringRedisTemplate.opsForSet().members(indexKey);
        if (sessionKeys != null && !sessionKeys.isEmpty()) {
            stringRedisTemplate.delete(sessionKeys);
        }
        stringRedisTemplate.delete(indexKey);
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

    private String userSessionsKey(String userId) {
        return USER_REFRESH_SESSIONS_PREFIX + userId;
    }

    private RefreshSession parseRefreshSession(String value) {
        if (value == null) return null;
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) return null;
        try {
            return new RefreshSession(parts[0], Integer.parseInt(parts[1]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record RefreshSession(String userId, int authVersion) { }
}
