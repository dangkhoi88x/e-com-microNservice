package com.example.microserviceecom.service;

import com.example.microserviceecom.dto.AuthenticationTokens;
import com.example.microserviceecom.dto.request.AuthenticationRequest;
import com.example.microserviceecom.dto.request.IntrospecRequest;
import com.example.microserviceecom.dto.response.IntrospectResponse;
import com.example.microserviceecom.entity.User;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.exception.ErrorCode;
import com.example.microserviceecom.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AuthenticationTokens authenticate(AuthenticationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        //Tạo authentication object
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password());

        Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
        //Lấy role
        var user = (User) authentication.getPrincipal();

        Collection<? extends GrantedAuthority> grantedAuthorities = user.getAuthorities();

        List<String> roles = grantedAuthorities.stream().map(GrantedAuthority::getAuthority).toList();
        //set access,rEfresh
        String accessToken = jwtService.generateAccessToken(user.getId(), roles, user.getAuthVersion());
        String refreshToken = tokenService.createRefreshToken(user.getId(), Duration.ofDays(14));

        return new AuthenticationTokens(user.getId(), accessToken, refreshToken);
    }

    public AuthenticationTokens refreshToken(String refreshToken) {
        if(refreshToken == null) {
            throw new AuthenticationException(ErrorCode.MISSING_REFRESH_TOKEN);
        }

        try {
            String userId = tokenService.findUserIdByRefreshToken(refreshToken);
            if (userId == null) {
                throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User does not exist"));

            List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

            String accessToken = jwtService.generateAccessToken(user.getId(), roles, user.getAuthVersion());

            return new AuthenticationTokens(user.getId(), accessToken, refreshToken);
        } catch (RuntimeException exception) {
            if (exception instanceof AuthenticationException authenticationException) {
                throw authenticationException;
            }
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                SignedJWT accessJwt = jwtService.verifyAccessToken(accessToken);
                tokenService.saveToken(
                        accessJwt.getJWTClaimsSet().getJWTID(),
                        accessJwt.getJWTClaimsSet().getSubject(),
                        accessJwt.getJWTClaimsSet().getExpirationTime().toInstant()
                );
            } catch (JwtException | ParseException | JOSEException exception) {
                log.warn("Could not revoke access token during logout: {}", exception.getMessage());
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenService.deleteRefreshSession(refreshToken);
        }
    }
    public IntrospectResponse introspect(IntrospecRequest request){
        try {
            SignedJWT signedJWT = jwtService.verifyAccessToken(request.getToken());
            String userId = signedJWT.getJWTClaimsSet().getSubject();
            return IntrospectResponse.builder()
                    .userId(userId)
                    .isValid(true)
                    .build();
        } catch (ParseException  | JOSEException |AuthenticationException e) {
            return IntrospectResponse.builder()
                    .isValid(false)
                    .build();
        }

    }
}
