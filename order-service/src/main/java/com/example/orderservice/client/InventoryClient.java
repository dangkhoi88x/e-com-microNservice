package com.example.orderservice.client;

import com.example.orderservice.dto.request.InventoryOrderRequest;
import com.example.orderservice.dto.request.ReserveInventoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    public void reserveInventory(ReserveInventoryRequest request, String token) {
        webClientBuilder.build()
                .post()
                .uri("http://INVENTORY-SERVICE/api/v1/inventory/reserve")
                .headers(headers -> headers.setBearerAuth(token))
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void releaseInventory(InventoryOrderRequest request, String token) {
        webClientBuilder.build()
                .post()
                .uri("http://INVENTORY-SERVICE/api/v1/inventory/release")
                .headers(headers -> headers.setBearerAuth(token))
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
