package com.example.cartservice.service.implement;

import com.example.cartservice.client.ProductClient;
import com.example.cartservice.dto.request.AddCartItemRequest;
import com.example.cartservice.dto.request.UpdateCartItemRequest;
import com.example.cartservice.dto.response.CartItemResponse;
import com.example.cartservice.dto.response.CartResponse;
import com.example.cartservice.dto.response.ProductSnapshot;
import com.example.cartservice.entity.Cart;
import com.example.cartservice.entity.CartItem;
import com.example.cartservice.enums.CartStatus;
import com.example.cartservice.exception.CartServiceException;
import com.example.cartservice.exception.ErrorCode;
import com.example.cartservice.repository.CartItemRepository;
import com.example.cartservice.repository.CartRepository;
import com.example.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import com.example.cartservice.dto.request.CartCheckoutRequest;
import com.example.cartservice.dto.response.CheckoutCartItemResponse;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;

    @Override
    public CartResponse getMyCart(String userId) {
        return toCartResponse(findActiveCartOrCreate(userId));
    }

    @Override
    public CartResponse addItem(String userId, AddCartItemRequest request) {
        Cart cart = findActiveCartOrCreate(userId);
        ProductSnapshot snapshot = productClient.getSnapshot(request);

        CartItem item = cartItemRepository
                .findByCartIdAndProductIdAndVariantId(cart.getId(), request.productId(), request.variantId())
                .orElse(null);

        if (item == null) {
            item = CartItem.builder()
                    .cart(cart)
                    .productId(snapshot.productId())
                    .variantId(snapshot.variantId())
                    .productName(snapshot.productName())
                    .variantName(snapshot.variantName())
                    .priceSnapshot(snapshot.price())
                    .imageUrl(snapshot.imageUrl())
                    .quantity(request.quantity())
                    .selected(true)
                    .build();
            cart.getItems().add(item);
        } else {
            item.setQuantity(item.getQuantity() + request.quantity());
            item.setProductName(snapshot.productName());
            item.setVariantName(snapshot.variantName());
            item.setPriceSnapshot(snapshot.price());
            item.setImageUrl(snapshot.imageUrl());
        }

        return toCartResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse updateItem(String userId, String itemId, UpdateCartItemRequest request) {
        Cart cart = findActiveCartOrCreate(userId);
        CartItem item = findCartItem(itemId);

        verifyCartOwnership(cart, item);
        item.setQuantity(request.quantity());
        if (request.selected() != null) {
            item.setSelected(request.selected());
        }

        return toCartResponse(cartRepository.save(cart));
    }

    @Override
    public void removeItem(String userId, String itemId) {
        Cart cart = findActiveCartOrCreate(userId);
        CartItem item = findCartItem(itemId);

        verifyCartOwnership(cart, item);
        cart.getItems().remove(item);
        cartRepository.save(cart);
    }

    @Override
    public void clearMyCart(String userId) {
        Cart cart = findActiveCartOrCreate(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Override
    public List<CheckoutCartItemResponse> getCheckoutItems(String userId) {
        return findActiveCartOrCreate(userId).getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getSelected()))
                .filter(item -> item.getCheckoutOrderId() == null)
                .map(item -> new CheckoutCartItemResponse(item.getId().toString(), item.getProductId(), item.getVariantId(), item.getQuantity()))
                .toList();
    }

    @Override
    public void markCheckout(String userId, CartCheckoutRequest request) {
        Cart cart = findActiveCartOrCreate(userId);
        Set<String> ids = new HashSet<>(request.itemIds());
        List<CartItem> items = cart.getItems().stream().filter(item -> ids.contains(item.getId().toString())).toList();
        if (items.size() != ids.size() || items.stream().anyMatch(item -> !Boolean.TRUE.equals(item.getSelected()) || item.getCheckoutOrderId() != null)) {
            throw new CartServiceException(ErrorCode.FORBIDDEN);
        }
        items.forEach(item -> item.setCheckoutOrderId(request.orderId()));
        cartRepository.save(cart);
    }

    @Override
    public void finalizeCheckout(String userId, String orderId) {
        Cart cart = findActiveCartOrCreate(userId);
        cart.getItems().removeIf(item -> orderId.equals(item.getCheckoutOrderId()));
        cartRepository.save(cart);
    }

    @Override
    public void releaseCheckout(String userId, String orderId) {
        Cart cart = findActiveCartOrCreate(userId);
        cart.getItems().stream().filter(item -> orderId.equals(item.getCheckoutOrderId())).forEach(item -> item.setCheckoutOrderId(null));
        cartRepository.save(cart);
    }

    private Cart findActiveCartOrCreate(String userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .userId(userId)
                        .status(CartStatus.ACTIVE)
                        .build()));
    }

    private CartItem findCartItem(String itemId) {
        try {
            return cartItemRepository.findById(UUID.fromString(itemId))
                    .orElseThrow(() -> new CartServiceException(ErrorCode.CART_ITEM_NOT_FOUND));
        } catch (IllegalArgumentException exception) {
            throw new CartServiceException(ErrorCode.INVALID_CART_ITEM_ID);
        }
    }

    private void verifyCartOwnership(Cart cart, CartItem item) {
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new CartServiceException(ErrorCode.FORBIDDEN);
        }
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .toList();

        BigDecimal totalAmount = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.selected()))
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.selected()))
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

    private CartItemResponse toCartItemResponse(CartItem item) {
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
