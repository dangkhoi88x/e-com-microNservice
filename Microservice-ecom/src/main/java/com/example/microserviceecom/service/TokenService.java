package com.example.microserviceecom.service;

import com.example.microserviceecom.entity.Token;
import com.example.microserviceecom.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final TokenRepository tokenRepository;

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

}
