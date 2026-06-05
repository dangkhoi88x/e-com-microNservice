package com.example.microserviceecom.service;

import com.example.microserviceecom.common.TokenType;
import com.example.microserviceecom.dto.TokenPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {


    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public String generateAccessToken(String userId, List<String> roles) {
        // Header
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        // Payload
        Instant now = Instant.now();

        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .subject(userId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .issuer("http://localhost:8080")
                .claim("roles", roles)
                .claim("typ", TokenType.ACCESS)
                .id(UUID.randomUUID().toString())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, jwtClaimsSet)).getTokenValue();
    }

    public TokenPayload generateRefreshToken(String userId) {
        // Header
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        // Payload
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600L * 24 * 14);
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .subject(userId)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .issuer("http://localhost:8080")
                .claim("typ", TokenType.REFRESH)
                .id(jti)
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, jwtClaimsSet)).getTokenValue();
        return TokenPayload.builder()
                .tokenValue(token)
                .userId(userId)
                .jti(jti)
                .expiration(expiresAt)
                .build();
    }

    public TokenPayload validateToken(String token, TokenType type) {
        Jwt jwt = jwtDecoder.decode(token);
        String typ = jwt.getClaim("typ").toString();
        if(TokenType.valueOf(typ) != type) {
            throw new JwtException("Invalid token type");
        }

        String userId = jwt.getSubject();
        List<String> roles = extractRoles(jwt.getClaim("roles"));
        String jti = jwt.getId();
        Instant issuedAt = jwt.getIssuedAt();
        Instant expiration = jwt.getExpiresAt();

        return TokenPayload.builder()
                .userId(userId)
                .roles(roles)
                .jti(jti)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .build();
    }

    private List<String> extractRoles(Object claimRoles) {
        if(claimRoles == null)
            return Collections.emptyList();

        if(claimRoles instanceof List<?> listRole) {
            return listRole.stream().map(String::valueOf)
                    .toList();
        }

        return Collections.emptyList();
    }

}
