package com.example.orderservice.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartClient {
    private final WebClient.Builder webClientBuilder;
    private static final String BASE = "http://CART-SERVICE/api/v1/cart/internal/carts/{userId}";

    public List<CartItem> checkoutItems(String userId) {
        ApiResponse<List<CartItem>> response = webClientBuilder.build().get().uri(BASE + "/checkout-items", userId)
                .retrieve().bodyToMono(new ParameterizedTypeReference<ApiResponse<List<CartItem>>>() {}).block();
        return response == null || response.data() == null ? List.of() : response.data();
    }
    public void mark(String userId, String orderId, List<String> itemIds) { post(userId, "/checkout", new CheckoutRequest(orderId, itemIds)); }
    public void finalize(String userId, String orderId) { post(userId, "/checkout/" + orderId + "/finalize", null); }
    public void release(String userId, String orderId) { post(userId, "/checkout/" + orderId + "/release", null); }
    private void post(String userId, String suffix, Object body) {
        WebClient.RequestBodySpec request = webClientBuilder.build().post().uri(BASE + suffix, userId);
        if (body != null) request.bodyValue(body);
        request.retrieve().toBodilessEntity().block();
    }
    private record ApiResponse<T>(T data) {}
    public record CartItem(String id, String productId, String variantId, Integer quantity) {}
    private record CheckoutRequest(String orderId, List<String> itemIds) {}
}
