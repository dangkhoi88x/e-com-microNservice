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
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class WishlistServiceImpl implements WishlistService {
    private final WishlistItemRepository repository;
    private final ProductClient productClient;
    @Override @Transactional(readOnly = true) public List<WishlistItemResponse> getMyWishlist(String userId) { return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::response).toList(); }
    @Override public WishlistItemResponse addItem(String userId, AddWishlistItemRequest request) {
        ProductSnapshot snapshot = productClient.getSnapshot(request.productId(), request.variantId());
        return repository.findByUserIdAndProductIdAndVariantId(userId, snapshot.productId(), snapshot.variantId())
                .map(this::response)
                .orElseGet(() -> response(repository.save(WishlistItem.builder()
                        .userId(userId).productId(snapshot.productId()).variantId(snapshot.variantId())
                        .productName(snapshot.productName()).price(snapshot.price()).imageUrl(snapshot.imageUrl())
                        .categoryName(snapshot.categoryName()).build())));
    }
    @Override public void removeItem(String userId, String productId, String variantId) { repository.findByUserIdAndProductIdAndVariantId(userId, productId, variantId).ifPresent(repository::delete); }
    @Override public void clear(String userId) { repository.deleteByUserId(userId); }
    private WishlistItemResponse response(WishlistItem item) { return new WishlistItemResponse(item.getId().toString(), item.getProductId(), item.getVariantId(), item.getProductName(), item.getPrice(), item.getImageUrl(), item.getCategoryName(), item.getCreatedAt()); }
}
