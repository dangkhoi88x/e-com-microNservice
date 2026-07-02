package com.example.microserviceecom.service;

import com.example.microserviceecom.common.TokenType;
import com.example.microserviceecom.dto.TokenPayload;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.exception.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {
    @Value("${jwt.secret-key}")
    private String secretKey;

    private final TokenService tokenService;

    public String generateAccessToken(String userId, List<String> roles) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        Date now = new Date();
        Date expirationTime = Date.from(now.toInstant().plus(1, ChronoUnit.HOURS));

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(now)
                .expirationTime(expirationTime)
                .issuer("http://localhost:8080")
                .claim("roles", roles)
                .claim("typ", TokenType.ACCESS.name())
                .jwtID(UUID.randomUUID().toString())
                .build();

        return signToken(header, jwtClaimsSet);
    }

    public TokenPayload generateRefreshToken(String userId) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        Date now = new Date();
        Instant expiresAt = now.toInstant().plus(14, ChronoUnit.DAYS);
        Date expirationTime = Date.from(expiresAt);
        String jti = UUID.randomUUID().toString();

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(now)
                .expirationTime(expirationTime)
                .issuer("http://localhost:8080")
                .claim("typ", TokenType.REFRESH.name())
                .jwtID(jti)
                .build();

        String token = signToken(header, jwtClaimsSet);
        return TokenPayload.builder()
                .tokenValue(token)
                .userId(userId)
                .jti(jti)
                .expiration(expiresAt)
                .build();
    }

    public SignedJWT verifyAccessToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        boolean isValid = signedJWT.verify(new MACVerifier(getSecretKeyBytes()));
        if (!isValid) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        if (expirationTime == null || expirationTime.before(new Date())) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        TokenType type = TokenType.valueOf(signedJWT.getJWTClaimsSet().getClaim("typ").toString());
        if (type != TokenType.ACCESS) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        String jti = signedJWT.getJWTClaimsSet().getJWTID();
        if (tokenService.findByJti(jti) != null) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        return signedJWT;
    }

    public SignedJWT verifyRefreshToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        boolean isValid = signedJWT.verify(new MACVerifier(getSecretKeyBytes()));
        if (!isValid) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        if (expirationTime == null || expirationTime.before(new Date())) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        TokenType type = TokenType.valueOf(signedJWT.getJWTClaimsSet().getClaim("typ").toString());
        if (type != TokenType.REFRESH) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        String jti = signedJWT.getJWTClaimsSet().getJWTID();
        if (tokenService.findByJti(jti) == null) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        return signedJWT;
    }

    private String signToken(JWSHeader header, JWTClaimsSet jwtClaimsSet) {
        SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);
        try {
            signedJWT.sign(new MACSigner(getSecretKeyBytes()));
            return signedJWT.serialize();
        } catch (JOSEException exception) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }
    }

    private byte[] getSecretKeyBytes() {
        return secretKey.getBytes(StandardCharsets.UTF_8);
    }

}
