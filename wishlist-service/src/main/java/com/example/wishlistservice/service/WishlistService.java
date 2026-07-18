package com.example.wishlistservice.service;

import com.example.wishlistservice.dto.request.AddWishlistItemRequest;
import com.example.wishlistservice.dto.response.WishlistItemResponse;
import java.util.List;

public interface WishlistService {
    List<WishlistItemResponse> getMyWishlist(String userId);
    WishlistItemResponse addItem(String userId, AddWishlistItemRequest request);
    void removeItem(String userId, String productId, String variantId);
    void clear(String userId);
}
