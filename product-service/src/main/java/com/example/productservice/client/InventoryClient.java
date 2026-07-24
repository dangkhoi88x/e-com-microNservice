package com.example.productservice.client;

import com.example.productservice.dto.request.BatchInventoryRequest;
import com.example.productservice.dto.response.InventoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "PRODUCT-INVENTORY-CLIENT")
public class InventoryClient {

    // Tạo RestClient riêng cho InventoryClient, KHÔNG lấy từ Spring bean dùng chung
    // để tránh bị Eureka client "mượn" RestClient.Builder và bị LoadBalancerInterceptor can thiệp.
    private final RestClient restClient;
    private final String inventoryServiceBaseUrl;

    public InventoryClient(
            @Value("${inventory-service.base-url:http://localhost:8087}") String inventoryServiceBaseUrl
    ) {
        this.restClient = RestClient.builder().build();
        this.inventoryServiceBaseUrl = inventoryServiceBaseUrl;
    }

    public Optional<Integer> getAvailableQuantity(String productId) {
        try {
            InventoryResponse response = restClient
                    .get()
                    .uri(inventoryServiceBaseUrl + "/api/v1/inventory/products/{productId}", productId)
                    .retrieve()
                    .body(InventoryResponse.class);

            return Optional.ofNullable(response)
                    .map(InventoryResponse::availableQuantity);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                log.warn("Inventory not found for productId={}", productId);
                return Optional.empty();
            }

            log.warn("Failed to fetch inventory for productId={}", productId, exception);
            return Optional.empty();
        } catch (RestClientException exception) {
            log.warn("Failed to fetch inventory for productId={}", productId, exception);
            return Optional.empty();
        }
    }

    public Map<String, Integer> getAvailableQuantities(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<InventoryResponse> responses = restClient
                    .post()
                    .uri(inventoryServiceBaseUrl + "/api/v1/inventory/products/batch")
                    .body(new BatchInventoryRequest(productIds))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (responses == null) {
                return Collections.emptyMap();
            }

            return responses.stream()
                    .collect(Collectors.toMap(
                            InventoryResponse::productId,
                            InventoryResponse::availableQuantity,
                            (left, right) -> right
                    ));
        } catch (RestClientException exception) {
            log.warn("Failed to fetch inventories for productIds={}", productIds, exception);
            return Collections.emptyMap();
        }
    }

    public int setAvailableQuantity(String productId, int quantity) {
        try {
            InventoryResponse response = restClient.put()
                    .uri(inventoryServiceBaseUrl + "/internal/inventory/products/{productId}/quantity", productId)
                    .body(Map.of("availableQuantity", quantity))
                    .retrieve()
                    .body(InventoryResponse.class);
            if (response == null || response.availableQuantity() == null) throw new IllegalStateException("Inventory update returned no quantity");
            return response.availableQuantity();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Inventory Service is unavailable", exception);
        }
    }
}
