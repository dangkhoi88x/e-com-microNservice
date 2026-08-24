package com.example.cartservice.controller;

import com.example.cartservice.dto.request.CartCheckoutRequest;
import com.example.cartservice.dto.response.ApiResponse;
import com.example.cartservice.dto.response.CheckoutCartItemResponse;
import com.example.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Trusted endpoints consumed by order-service.
 *
 * <p>These take a {@code userId} straight from the path with no ownership check, so this prefix
 * must never be exposed through the API Gateway. Only {@code /api/v1/cart/**} is routed publicly.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/cart")
public class InternalCartController {

    private final CartService cartService;

    @GetMapping("/users/{userId}/checkout-items")
    public ApiResponse<List<CheckoutCartItemResponse>> checkoutItems(@PathVariable String userId) {
        return ApiResponse.<List<CheckoutCartItemResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Checkout items retrieved")
                .data(cartService.getCheckoutItems(userId))
                .build();
    }

    @PostMapping("/users/{userId}/checkout")
    public ApiResponse<Void> markCheckout(@PathVariable String userId, @RequestBody @Valid CartCheckoutRequest request) {
        cartService.markCheckout(userId, request);
        return response("Cart checkout marked");
    }

    @PostMapping("/{orderId}/finalize")
    public ApiResponse<Void> finalizeCheckout(@PathVariable String orderId) {
        cartService.finalizeCheckout(orderId);
        return response("Cart checkout finalized");
    }

    @PostMapping("/{orderId}/release")
    public ApiResponse<Void> releaseCheckout(@PathVariable String orderId) {
        cartService.releaseCheckout(orderId);
        return response("Cart checkout released");
    }

    private ApiResponse<Void> response(String message) {
        return ApiResponse.<Void>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .message(message)
                .build();
    }
}
