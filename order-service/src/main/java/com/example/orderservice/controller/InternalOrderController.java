package com.example.orderservice.controller;

import com.example.orderservice.dto.response.ApiResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.ReviewEligibilityResponse;
import com.example.orderservice.exception.ErrorCode;
import com.example.orderservice.exception.OrderServiceException;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/orders")
public class InternalOrderController {
    private final OrderService orderService;

    @Value("${services.payment.internal-api-key}")
    private String paymentInternalApiKey;

    @GetMapping("/{orderId}/payment-validation")
    public ApiResponse<OrderResponse> getOrderForPaymentValidation(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @PathVariable String orderId
    ) {
        if (!matchesInternalApiKey(apiKey)) {
            throw new OrderServiceException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order retrieved for payment validation")
                .data(orderService.getOrderForPaymentValidation(orderId))
                .build();
    }

    @GetMapping("/items/{orderItemId}/review-eligibility")
    public ReviewEligibilityResponse checkReviewEligibility(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderItemId
    ) {
        return orderService.checkReviewEligibility(jwt.getSubject(), orderItemId);
    }

    private boolean matchesInternalApiKey(String candidate) {
        if (candidate == null || paymentInternalApiKey == null) {
            return false;
        }

        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                paymentInternalApiKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
