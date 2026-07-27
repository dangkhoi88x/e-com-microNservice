package com.example.orderservice.controller;

import com.example.orderservice.dto.response.AdminAnalyticsResponse;
import com.example.orderservice.dto.response.ApiResponse;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders/admin/analytics")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAnalyticsController {
    private final OrderService service;

    @GetMapping
    public ApiResponse<AdminAnalyticsResponse> get(@RequestParam Instant from, @RequestParam Instant to) {
        if (!from.isBefore(to)) throw new IllegalArgumentException("from must be before to");
        return ApiResponse.<AdminAnalyticsResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Admin analytics retrieved successfully")
                .data(service.getAdminAnalytics(from, to))
                .build();
    }
}
