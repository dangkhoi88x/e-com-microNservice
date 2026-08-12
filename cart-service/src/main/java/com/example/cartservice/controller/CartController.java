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

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status.value())
                .message(message)
                .data(data)
                .build();
    }
}
