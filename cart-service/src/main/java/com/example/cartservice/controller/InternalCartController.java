package com.example.cartservice.controller;

import com.example.cartservice.dto.response.ApiResponse;
import com.example.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Trusted endpoint consumed by order-service after a payment event. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/cart")
public class InternalCartController {

    private final CartService cartService;

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
