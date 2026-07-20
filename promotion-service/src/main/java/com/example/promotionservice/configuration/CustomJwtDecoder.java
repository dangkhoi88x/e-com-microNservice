package com.example.promotionservice.configuration;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            SignedJWT signed = SignedJWT.parse(token);
            var claims = signed.getJWTClaimsSet();
            Instant issuedAt = claims.getIssueTime() == null ? Instant.now() : claims.getIssueTime().toInstant();
            Instant expiresAt = claims.getExpirationTime() == null ? issuedAt.plusSeconds(3600) : claims.getExpirationTime().toInstant();
            return new Jwt(token, issuedAt, expiresAt, signed.getHeader().toJSONObject(), claims.getClaims());
        } catch (ParseException exception) {
            throw new JwtException("Invalid JWT", exception);
        }
    }
}
