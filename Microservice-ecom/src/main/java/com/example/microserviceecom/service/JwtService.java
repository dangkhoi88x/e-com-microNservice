package com.example.microserviceecom.service;

import com.example.microserviceecom.common.TokenType;
import com.example.microserviceecom.configuration.JwtKeyProvider;
import com.example.microserviceecom.dto.TokenPayload;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.exception.ErrorCode;
import com.example.microserviceecom.repository.UserRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience:}")
    private String audience;

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final JwtKeyProvider jwtKeyProvider;
    //tao jwt
    public String generateAccessToken(String userId, List<String> roles, int authVersion) {
        JWSHeader header = signingHeader();
        Date now = new Date();
        Date expirationTime = Date.from(now.toInstant().plus(15, ChronoUnit.MINUTES));

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(now)
                .expirationTime(expirationTime)
                .issuer(issuer)
                .audience(audience)
                .claim("roles", roles)
                .claim("ver", authVersion)
                .claim("typ", TokenType.ACCESS.name())
                .jwtID(UUID.randomUUID().toString())
                .build();

        return signToken(header, jwtClaimsSet);
    }

    public TokenPayload generateRefreshToken(String userId) {
        JWSHeader header = signingHeader();
        Date now = new Date();
        Instant expiresAt = now.toInstant().plus(14, ChronoUnit.DAYS);
        Date expirationTime = Date.from(expiresAt);
        String jti = UUID.randomUUID().toString();

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(now)
                .expirationTime(expirationTime)
                .issuer(issuer)
                .audience(audience)
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
//Xác minh access token
    public SignedJWT verifyAccessToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        verifySignatureAndIssuer(signedJWT);

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

        String userId = signedJWT.getJWTClaimsSet().getSubject();
        Integer tokenVersion = signedJWT.getJWTClaimsSet().getIntegerClaim("ver");
        if (tokenVersion == null) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }
        boolean currentVersion = userRepository.findById(userId)
                .map(user -> user.getAuthVersion() != null && user.getAuthVersion() == tokenVersion)
                .orElse(false);
        if (!currentVersion) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        return signedJWT;
    }

    public SignedJWT verifyRefreshToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        verifySignatureAndIssuer(signedJWT);

        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        if (expirationTime == null || expirationTime.before(new Date())) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        TokenType type = TokenType.valueOf(signedJWT.getJWTClaimsSet().getClaim("typ").toString());
        if (type != TokenType.REFRESH) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }

        return signedJWT;
    }

    private String signToken(JWSHeader header, JWTClaimsSet jwtClaimsSet) {
        SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);
        try {
            signedJWT.sign(new RSASSASigner(jwtKeyProvider.signingJwk().toPrivateKey()));
            return signedJWT.serialize();
        } catch (JOSEException exception) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }
    }

    private JWSHeader signingHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(jwtKeyProvider.signingJwk().getKeyID())
                .build();
    }

    private void verifySignatureAndIssuer(SignedJWT signedJWT) throws JOSEException, ParseException {
        if (!JWSAlgorithm.RS256.equals(signedJWT.getHeader().getAlgorithm())
                || !signedJWT.verify(new RSASSAVerifier(jwtKeyProvider.signingJwk().toRSAPublicKey()))
                || !issuer.equals(signedJWT.getJWTClaimsSet().getIssuer())
                || (!audience.isBlank() && !signedJWT.getJWTClaimsSet().getAudience().contains(audience))) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }
    }

}
