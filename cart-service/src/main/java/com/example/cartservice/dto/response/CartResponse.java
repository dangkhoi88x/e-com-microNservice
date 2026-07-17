package com.example.cartservice.dto.response;

import com.example.cartservice.enums.CartStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder
public record CartResponse(
        String id,
        String userId,
        CartStatus status,
        List<CartItemResponse> items,
        Integer totalItems,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt
) {
}
