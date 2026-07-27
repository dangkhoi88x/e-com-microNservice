package com.example.cartservice.repository;

import com.example.cartservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findAllByCartIdAndProductIdAndVariantId(
            UUID cartId,
            String productId,
            String variantId
    );

    List<CartItem> findByCheckoutOrderId(String checkoutOrderId);
}
