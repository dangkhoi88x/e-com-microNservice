package com.example.microserviceecom.controller;

import com.example.microserviceecom.dto.request.AuthenticationRequest;
import com.example.microserviceecom.dto.response.ApiResponse;
import com.example.microserviceecom.dto.response.AuthenticationResponse;
import com.example.microserviceecom.service.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;


    @PostMapping("/login")


    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request,
                                                            HttpServletResponse response) {
        var result = authenticationService.authenticate(request);

        Cookie cookie = new Cookie("refresh_token", result.refreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setMaxAge(3600 * 24 * 14);
        cookie.setDomain("localhost");
        cookie.setPath("/");

        response.addCookie(cookie);

        return ApiResponse.<AuthenticationResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Login success")
                .data(result)
                .build();
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthenticationResponse> refreshToken(@CookieValue(name = "refresh_token") String refreshToken) {
        var data = authenticationService.refreshToken(refreshToken);
        return ApiResponse.<AuthenticationResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Refresh Token success")
                .data(data)
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader,
                       @CookieValue(name = "refresh_token") String refreshToken,
                       HttpServletResponse response) {
        String token = authHeader.replace("Bearer ", "");

        authenticationService.logout(token, refreshToken);
        response.addCookie(new Cookie("refresh_token", null));

        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Logout successful")
                .build();
    }
}
