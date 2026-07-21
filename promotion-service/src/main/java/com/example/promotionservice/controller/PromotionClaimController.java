package com.example.promotionservice.controller;

import com.example.promotionservice.dto.response.ApiResponse;
import com.example.promotionservice.dto.response.PromotionCampaignResponse;
import com.example.promotionservice.service.PromotionUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/promotions/claims")
public class PromotionClaimController {
    private final PromotionUsageService service;

    @PostMapping("/{campaignId}")
    public ApiResponse<PromotionCampaignResponse> claim(@PathVariable String campaignId, @AuthenticationPrincipal Jwt jwt) {
        return response(HttpStatus.OK, "Promotion claimed successfully", service.claim(campaignId, jwt.getSubject()));
    }

    @GetMapping
    public ApiResponse<List<PromotionCampaignResponse>> getClaimed(@AuthenticationPrincipal Jwt jwt) {
        return response(HttpStatus.OK, "Claimed promotions retrieved successfully", service.getClaimed(jwt.getSubject()));
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).data(data).build();
    }
}
