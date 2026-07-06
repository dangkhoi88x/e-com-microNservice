package com.example.notificationservice.controller;

import com.example.notificationservice.dto.res.ApiResponse;
import com.example.notificationservice.dto.res.NotificationResponse;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping({"/me", "/my-notifications"})
    public ApiResponse<List<NotificationResponse>> myNotifications(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<NotificationResponse> data = notificationService.myNotifications(userId);

        return ApiResponse.<List<NotificationResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("My notifications successfully")
                .data(data)
                .build();
    }
}
