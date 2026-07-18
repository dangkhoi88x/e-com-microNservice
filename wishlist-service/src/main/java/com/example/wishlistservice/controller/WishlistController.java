package com.example.wishlistservice.controller;

import com.example.wishlistservice.dto.request.AddWishlistItemRequest;
import com.example.wishlistservice.dto.response.*;
import com.example.wishlistservice.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/wishlist")
public class WishlistController {
    private final WishlistService service;
    @GetMapping public ApiResponse<List<WishlistItemResponse>> getMyWishlist(@AuthenticationPrincipal Jwt jwt) { return response(HttpStatus.OK, "Wishlist retrieved successfully", service.getMyWishlist(jwt.getSubject())); }
    @PostMapping("/items") public ApiResponse<WishlistItemResponse> add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddWishlistItemRequest request) { return response(HttpStatus.CREATED, "Item added to wishlist", service.addItem(jwt.getSubject(), request)); }
    @DeleteMapping("/items/{productId}") public ApiResponse<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable String productId, @RequestParam(required = false) String variantId) { service.removeItem(jwt.getSubject(), productId, variantId); return response(HttpStatus.OK, "Item removed from wishlist", null); }
    @DeleteMapping public ApiResponse<Void> clear(@AuthenticationPrincipal Jwt jwt) { service.clear(jwt.getSubject()); return response(HttpStatus.OK, "Wishlist cleared", null); }
    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) { return ApiResponse.<T>builder().status(status.value()).message(message).data(data).build(); }
}
