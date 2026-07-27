package com.example.microserviceecom.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Holds the Identity service signing key; no downstream service receives this key. */
@Component
public class JwtKeyProvider {

    private final RSAKey signingJwk;

    public JwtKeyProvider(
            @Value("${jwt.rsa.private-key}") String privateKeyBase64,
            @Value("${jwt.rsa.key-id}") String keyId
    ) {
        try {
            byte[] encoded = Base64.getDecoder().decode(privateKeyBase64.replaceAll("\\s", ""));
            String pem = new String(encoded, StandardCharsets.US_ASCII);
            if (pem.startsWith("-----BEGIN PRIVATE KEY-----")) {
                String pemBody = pem
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                encoded = Base64.getDecoder().decode(pemBody);
            }
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(encoded));
            if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
                throw new IllegalArgumentException("JWT RSA private key must include CRT parameters");
            }
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));
            signingJwk = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyID(keyId)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Invalid jwt.rsa.private-key; provide a Base64-encoded PKCS#8 key or Base64-encoded PEM file",
                    exception
            );
        }
    }

    public RSAKey signingJwk() {
        return signingJwk;
    }

    public RSAKey publicJwk() {
        return signingJwk.toPublicJWK();
    }
}
