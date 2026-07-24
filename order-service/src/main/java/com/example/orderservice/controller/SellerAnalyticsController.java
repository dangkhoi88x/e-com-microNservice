package com.example.orderservice.controller;

import com.example.orderservice.dto.response.ApiResponse;
import com.example.orderservice.dto.response.SellerAnalyticsResponse;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/orders/seller/analytics") @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'SELLER')")
public class SellerAnalyticsController {
    private final OrderService service;
    @GetMapping public ApiResponse<SellerAnalyticsResponse> get(@AuthenticationPrincipal Jwt jwt, @RequestParam Instant from, @RequestParam Instant to) { return ApiResponse.<SellerAnalyticsResponse>builder().status(HttpStatus.OK.value()).message("Seller analytics retrieved successfully").data(service.getSellerAnalytics(jwt.getSubject(), from, to)).build(); }
}
