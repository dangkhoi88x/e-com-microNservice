package com.example.orderservice.controller;

import com.example.orderservice.dto.response.ReviewEligibilityResponse;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/orders")
public class InternalOrderController {
    private final OrderService orderService;

    @GetMapping("/items/{orderItemId}/review-eligibility")
    public ReviewEligibilityResponse checkReviewEligibility(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderItemId
    ) {
        return orderService.checkReviewEligibility(jwt.getSubject(), orderItemId);
    }
}
