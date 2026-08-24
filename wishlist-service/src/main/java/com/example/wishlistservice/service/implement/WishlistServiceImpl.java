package com.example.wishlistservice.service.implement;

import com.example.wishlistservice.dto.request.AddWishlistItemRequest;
import com.example.wishlistservice.dto.response.WishlistItemResponse;
import com.example.wishlistservice.client.ProductClient;
import com.example.wishlistservice.client.ProductSnapshot;
import com.example.wishlistservice.entity.WishlistItem;
import com.example.wishlistservice.repository.WishlistItemRepository;
import com.example.wishlistservice.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional
public class WishlistServiceImpl implements WishlistService {
    private final WishlistItemRepository repository;
    private final ProductClient productClient;
    private final JdbcTemplate jdbcTemplate;
    @Override @Transactional(readOnly = true) public List<WishlistItemResponse> getMyWishlist(String userId) { return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::response).toList(); }
    @Override public WishlistItemResponse addItem(String userId, AddWishlistItemRequest request) {
        ProductSnapshot snapshot = productClient.getSnapshot(request.productId(), request.variantId());
        String variantId = normalizeVariantId(snapshot.variantId());

        jdbcTemplate.update("""
                INSERT INTO wishlist_items (id, user_id, product_id, variant_id, product_name, price, image_url, category_name, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id, product_id, variant_id) DO NOTHING
                """,
                UUID.randomUUID(), userId, snapshot.productId(), variantId, snapshot.productName(), snapshot.price(),
                snapshot.imageUrl(), snapshot.categoryName());

        return repository.findByUserIdAndProductIdAndVariantId(userId, snapshot.productId(), variantId)
                .map(this::response)
                .orElseThrow();
    }
    @Override public void removeItem(String userId, String productId, String variantId)
    { repository.findByUserIdAndProductIdAndVariantId(userId, productId, normalizeVariantId(variantId)).ifPresent(repository::delete);
    }
    @Override public void clear(String userId)
    {
        repository.deleteByUserId(userId);
    }
    private String normalizeVariantId(String variantId)
    {
        return variantId == null ? "" : variantId;
    }
    private String nullableVariantId(String variantId)
    {
        return variantId.isEmpty() ? null : variantId;
    }
    private WishlistItemResponse response(WishlistItem item)
    {
        return new WishlistItemResponse(item.getId().toString(), item.getProductId(), nullableVariantId(item.getVariantId()), item.getProductName(), item.getPrice(), item.getImageUrl(), item.getCategoryName(), item.getCreatedAt());
    }
}
