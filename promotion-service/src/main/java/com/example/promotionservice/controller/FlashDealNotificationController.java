package com.example.promotionservice.controller;

import com.example.promotionservice.dto.response.ApiResponse;
import com.example.promotionservice.service.FlashDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/flash-deals")
public class FlashDealNotificationController {
    private final FlashDealService service;
    @PostMapping("/{id}/notifications") public ApiResponse<Void> subscribe(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) { service.subscribeForNotification(id, jwt.getSubject()); return response(HttpStatus.OK, null); }
    @GetMapping("/notification-subscriptions") public ApiResponse<List<String>> subscriptions(@AuthenticationPrincipal Jwt jwt) { return response(HttpStatus.OK, service.getNotificationSubscriptions(jwt.getSubject())); }
    @PostMapping("/notifications/general") public ApiResponse<Void> subscribeForGeneralNotifications(@AuthenticationPrincipal Jwt jwt) { service.subscribeForGeneralNotification(jwt.getSubject()); return response(HttpStatus.OK, null); }
    @GetMapping("/notifications/general") public ApiResponse<Boolean> generalNotificationSubscription(@AuthenticationPrincipal Jwt jwt) { return response(HttpStatus.OK, service.hasGeneralNotificationSubscription(jwt.getSubject())); }
    private <T> ApiResponse<T> response(HttpStatus status, T data) { return ApiResponse.<T>builder().status(status.value()).message("Flash Sale notification subscription updated").data(data).build(); }
}
