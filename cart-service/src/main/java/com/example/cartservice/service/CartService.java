package com.example.cartservice.service;

import com.example.cartservice.dto.request.AddCartItemRequest;
import com.example.cartservice.dto.request.UpdateCartItemRequest;
import com.example.cartservice.dto.response.CartResponse;
import com.example.cartservice.dto.response.CheckoutCartItemResponse;
import com.example.cartservice.dto.request.CartCheckoutRequest;
import java.util.List;

public interface CartService {
    CartResponse getMyCart(String userId);
    CartResponse addItem(String userId, AddCartItemRequest request);
    CartResponse updateItem(String userId, String itemId, UpdateCartItemRequest request);
    void removeItem(String userId, String itemId);
    void clearMyCart(String userId);
    List<CheckoutCartItemResponse> getCheckoutItems(String userId);
    void markCheckout(String userId, CartCheckoutRequest request);
    void finalizeCheckout(String userId, String orderId);
    void releaseCheckout(String userId, String orderId);
    void finalizeCheckout(String orderId);
    void releaseCheckout(String orderId);
}
