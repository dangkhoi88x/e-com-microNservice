package com.example.microserviceecom.controller;

import com.example.microserviceecom.dto.request.AuthenticationRequest;
import com.example.microserviceecom.dto.request.PasswordResetRequest;
import com.example.microserviceecom.dto.request.PasswordResetConfirmRequest;
import com.example.microserviceecom.dto.response.ApiResponse;
import com.example.microserviceecom.dto.response.AuthenticationResponse;
import com.example.microserviceecom.exception.AuthenticationException;
import com.example.microserviceecom.exception.ErrorCode;
import com.example.microserviceecom.service.AuthenticationService;
import com.example.microserviceecom.service.PasswordResetService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    @org.springframework.beans.factory.annotation.Value("${security.refresh-cookie.secure:false}")
    private boolean refreshCookieSecure;

    @org.springframework.beans.factory.annotation.Value("${security.refresh-cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @org.springframework.beans.factory.annotation.Value("${security.refresh-request.allowed-origins}")
    private String allowedRefreshOrigins;

    @PostMapping("/login")


    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request,
                                                             HttpServletResponse response) {
        var result = authenticationService.authenticate(request);

        addRefreshCookie(response, result.refreshToken(), Duration.ofDays(14));

        return ApiResponse.<AuthenticationResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login success")
                .data(new AuthenticationResponse(result.userId(), result.accessToken()))
                .build();
    }

    @PostMapping("/password-reset/request")
    public ApiResponse<Void> requestPasswordReset(
            @RequestBody @Valid PasswordResetRequest request
    ) {
        passwordResetService.request(request.email());
        return ApiResponse.<Void>builder().status(HttpStatus.OK.value())
                .message("Nếu email tồn tại, mã xác nhận đã được gửi").build();
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> confirmPasswordReset(@RequestBody @Valid PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request);
        return ApiResponse.<Void>builder().status(HttpStatus.OK.value())
                .message("Đặt lại mật khẩu thành công").build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthenticationResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            @RequestHeader(value = "Origin", required = false) String origin,
            HttpServletResponse response
    ) {
        validateRefreshOrigin(origin);
        var data = authenticationService.refreshToken(refreshToken);
        addRefreshCookie(response, data.refreshToken(), Duration.ofDays(14));
        return ApiResponse.<AuthenticationResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Refresh Token success")
                .data(new AuthenticationResponse(data.userId(), data.accessToken()))
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                       @CookieValue(name = "refresh_token", required = false) String refreshToken,
                       HttpServletResponse response) {
        String token = authHeader != null ? authHeader.replace("Bearer ", "") : null;

        authenticationService.logout(token, refreshToken);
        addRefreshCookie(response, "", Duration.ZERO);

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Logout successful")
                .build();
    }
    private void addRefreshCookie(HttpServletResponse response, String token, Duration maxAge) {
        clearLegacyRefreshCookie(response);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/identity/auth/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearLegacyRefreshCookie(HttpServletResponse response) {
        ResponseCookie legacyCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, legacyCookie.toString());
    }

    private void validateRefreshOrigin(String origin) {
        Set<String> allowedOrigins = Arrays.stream(allowedRefreshOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        if (origin == null || !allowedOrigins.contains(origin)) {
            throw new AuthenticationException(ErrorCode.INVALID_REFRESH_ORIGIN);
        }
    }

}
