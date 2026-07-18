package com.example.wishlistservice.configuration;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import java.text.ParseException;

@Component
public class CustomJwtDecoder implements JwtDecoder {
    @Override public Jwt decode(String token) throws JwtException {
        try { SignedJWT parsed = SignedJWT.parse(token); return new Jwt(token, parsed.getJWTClaimsSet().getIssueTime().toInstant(), parsed.getJWTClaimsSet().getExpirationTime().toInstant(), parsed.getHeader().toJSONObject(), parsed.getJWTClaimsSet().getClaims()); }
        catch (ParseException exception) { throw new JwtException("Invalid JWT", exception); }
    }
}
