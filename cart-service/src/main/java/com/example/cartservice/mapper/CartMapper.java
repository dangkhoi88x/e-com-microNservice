package com.example.cartservice.mapper;

import com.example.cartservice.dto.response.CartItemResponse;
import com.example.cartservice.dto.response.CartResponse;
import com.example.cartservice.dto.response.CheckoutCartItemResponse;
import com.example.cartservice.entity.Cart;
import com.example.cartservice.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Converts cart entities into API response DTOs.
 * Cart business rules remain in CartServiceImpl.
 */
@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        List<CartItemResponse> selectedItems = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.selected()))
                .toList();

        BigDecimal totalAmount = selectedItems.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = selectedItems.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId().toString())
                .userId(cart.getUserId())
                .status(cart.getStatus())
                .items(items)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public CheckoutCartItemResponse toCheckoutItemResponse(CartItem item) {
        return new CheckoutCartItemResponse(
                item.getId().toString(),
                item.getProductId(),
                item.getVariantId(),
                item.getQuantity()
        );
    }

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal subtotal = item.getPriceSnapshot()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId().toString())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .price(item.getPriceSnapshot())
                .imageUrl(item.getImageUrl())
                .quantity(item.getQuantity())
                .selected(item.getSelected())
                .subtotal(subtotal)
                .build();
    }
}
