package com.example.microserviceecom.controller;

import com.example.microserviceecom.configuration.JwtKeyProvider;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtKeyProvider jwtKeyProvider;
        //Các downstream service có thể dùng JWKS để tự xác minh JWT
    @GetMapping(value = "/.well-known/jwks.json", produces = "application/jwk-set+json")
    public Map<String, Object> getJwks() {
        return new JWKSet(jwtKeyProvider.publicJwk()).toJSONObject();
    }
}
