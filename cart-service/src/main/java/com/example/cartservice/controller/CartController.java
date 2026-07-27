package com.example.cartservice.controller;

import com.example.cartservice.dto.request.AddCartItemRequest;
import com.example.cartservice.dto.request.UpdateCartItemRequest;
import com.example.cartservice.dto.response.ApiResponse;
import com.example.cartservice.dto.response.CartResponse;
import com.example.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.cartservice.dto.request.CartCheckoutRequest;
import com.example.cartservice.dto.response.CheckoutCartItemResponse;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ApiResponse<CartResponse> getMyCart(@AuthenticationPrincipal Jwt jwt) {
        return response(HttpStatus.OK, "Cart retrieved successfully", cartService.getMyCart(jwt.getSubject()));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ApiResponse<CartResponse> addItem(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AddCartItemRequest request
    ) {
        return response(HttpStatus.OK, "Item added to cart successfully", cartService.addItem(jwt.getSubject(), request));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ApiResponse<CartResponse> updateItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String itemId,
            @RequestBody @Valid UpdateCartItemRequest request
    ) {
        return response(HttpStatus.OK, "Cart item updated successfully", cartService.updateItem(jwt.getSubject(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ApiResponse<Void> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String itemId
    ) {
        cartService.removeItem(jwt.getSubject(), itemId);
        return response(HttpStatus.OK, "Cart item removed successfully", null);
    }

    @DeleteMapping("/items")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ApiResponse<Void> clearMyCart(@AuthenticationPrincipal Jwt jwt) {
        cartService.clearMyCart(jwt.getSubject());
        return response(HttpStatus.OK, "Cart cleared successfully", null);
    }

    @GetMapping("/internal/carts/{userId}/checkout-items")
    public ApiResponse<List<CheckoutCartItemResponse>> checkoutItems(@PathVariable String userId) {
        return response(HttpStatus.OK, "Checkout items retrieved", cartService.getCheckoutItems(userId));
    }

    @PostMapping("/internal/carts/{userId}/checkout")
    public ApiResponse<Void> markCheckout(@PathVariable String userId, @RequestBody @Valid CartCheckoutRequest request) {
        cartService.markCheckout(userId, request);
        return response(HttpStatus.NO_CONTENT, "Cart checkout marked", null);
    }

    @PostMapping("/internal/carts/{userId}/checkout/{orderId}/finalize")
    public ApiResponse<Void> finalizeCheckout(@PathVariable String userId, @PathVariable String orderId) {
        cartService.finalizeCheckout(userId, orderId);
        return response(HttpStatus.NO_CONTENT, "Cart checkout finalized", null);
    }

    @PostMapping("/internal/carts/{userId}/checkout/{orderId}/release")
    public ApiResponse<Void> releaseCheckout(@PathVariable String userId, @PathVariable String orderId) {
        cartService.releaseCheckout(userId, orderId);
        return response(HttpStatus.NO_CONTENT, "Cart checkout released", null);
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .message(message)
                .data(data)
                .build();
    }
}
