package com.example.microserviceecom.security;

import com.example.microserviceecom.common.TokenType;
import com.example.microserviceecom.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomJwtValidator implements OAuth2TokenValidator<Jwt> {



    private final TokenRepository tokenRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String typ = jwt.getClaim("typ").toString();
        if(TokenType.valueOf(typ) != TokenType.ACCESS) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token_type", "Invalid token type", null));
        }

        String jti = jwt.getId();
        if(tokenRepository.existsById(jti)) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("token_logout", "Token ready logout", null));
        }

        return OAuth2TokenValidatorResult.success();
    }

}
