package com.example.wishlistservice.repository;

import com.example.wishlistservice.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<WishlistItem> findByUserIdAndProductIdAndVariantId(String userId, String productId, String variantId);
    long deleteByUserId(String userId);
}
