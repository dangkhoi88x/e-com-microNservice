package com.example.microserviceecom.service;

import com.example.microserviceecom.common.TokenType;
import com.example.microserviceecom.dto.TokenPayload;
import com.example.microserviceecom.dto.request.AuthenticationRequest;
import com.example.microserviceecom.dto.response.AuthenticationResponse;
import com.example.microserviceecom.entity.Token;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.exception.AppException;
import com.example.microserviceecom.exception.ErrorCode;
import com.example.microserviceecom.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.email(), request.password());

        Authentication authentication = this.authenticationManager.authenticate(authenticationToken);

        var user = (User) authentication.getPrincipal();

        Collection<? extends GrantedAuthority> grantedAuthorities = user.getAuthorities();

        List<String> roles = grantedAuthorities.stream().map(GrantedAuthority::getAuthority).toList();

        String accessToken = jwtService.generateAccessToken(user.getId(), roles);
        TokenPayload refreshToken = jwtService.generateRefreshToken(user.getId());

        tokenService.saveToken(refreshToken.jti(), user.getId(), refreshToken.expiration());

        return AuthenticationResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken.tokenValue())
                .build();
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        if(refreshToken == null) {
            throw new AppException(ErrorCode.MISSING_REFRESH_TOKEN);
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(refreshToken);
            boolean isValid = signedJWT.verify(new MACVerifier(secretKey));
            if(!isValid) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            var userId = signedJWT.getJWTClaimsSet().getSubject();
            var jti = signedJWT.getJWTClaimsSet().getJWTID();

            Token token = tokenService.findByJti(jti);
            if(token == null)
                throw new AppException(ErrorCode.UNAUTHORIZED);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User does not exist"));

            List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

            String accessToken = jwtService.generateAccessToken(userId, roles);

            return AuthenticationResponse.builder()
                    .userId(userId)
                    .accessToken(accessToken)
                    .build();
        }catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    public void logout(String accessToken, String refreshToken) {
        try {
            var payloadAccess = jwtService.validateToken(accessToken, TokenType.ACCESS);
            var payloadRefresh = jwtService.validateToken(refreshToken, TokenType.REFRESH);
            tokenService.saveToken(payloadAccess.jti(), payloadAccess.userId(), payloadAccess.expiration());
            tokenService.deleteToken(payloadRefresh.jti());
        } catch (JwtException e) {
            log.error("Invalid token: {}", e.getMessage());
        }
    }

}
