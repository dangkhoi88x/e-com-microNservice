package com.example.microserviceecom.configuration;

import com.nimbusds.jose.JOSEException;
import com.example.microserviceecom.security.CustomJwtValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;


@Configuration
@RequiredArgsConstructor
public class    JwtConfiguration {

    @Value("${jwt.issuer}")
    private String issuer;

    private final CustomJwtValidator customJwtValidator;
    private final JwtKeyProvider jwtKeyProvider;

    @Bean
    public JwtDecoder jwtDecoder() throws JOSEException {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(jwtKeyProvider.signingJwk().toRSAPublicKey()).build();

        OAuth2TokenValidator<Jwt> jwtTimestampValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withTokenTypeAndTimestamp =
                new DelegatingOAuth2TokenValidator<>(jwtTimestampValidator, customJwtValidator);

        jwtDecoder.setJwtValidator(withTokenTypeAndTimestamp);

        return jwtDecoder;
    }
}
