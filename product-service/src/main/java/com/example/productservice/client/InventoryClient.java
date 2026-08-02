package com.example.productservice.client;

import com.example.productservice.dto.request.BatchInventoryRequest;
import com.example.productservice.dto.response.InventoryResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
    private final CircuitBreaker inventoryCircuitBreaker;

    public InventoryClient(
            @Value("${inventory-service.base-url:http://localhost:8087}") String inventoryServiceBaseUrl,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.restClient = RestClient.builder().build();
        this.inventoryServiceBaseUrl = inventoryServiceBaseUrl;
        this.inventoryCircuitBreaker = circuitBreakerRegistry.circuitBreaker("inventoryService");
    }

    public Optional<Integer> getAvailableQuantity(String productId) {
        try {
            return inventoryCircuitBreaker.executeSupplier(() -> fetchAvailableQuantity(productId));
        } catch (CallNotPermittedException exception) {
            throw unavailable(productId, exception);
        } catch (RestClientException exception) {
            throw unavailable(productId, exception);
        }
    }

    public Map<String, Integer> getAvailableQuantities(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return inventoryCircuitBreaker.executeSupplier(() -> fetchAvailableQuantities(productIds));
        } catch (CallNotPermittedException exception) {
            log.warn("Inventory circuit breaker is open; rejecting batch inventory request");
            throw new InventoryClientException(exception);
        } catch (RestClientException exception) {
            log.warn("Inventory service is unavailable for productIds={}", productIds, exception);
            throw new InventoryClientException(exception);
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

    public int setVariantAvailableQuantity(String productId, String variantId, int quantity) {
        try {
            InventoryResponse response = restClient.put()
                    .uri(inventoryServiceBaseUrl + "/internal/inventory/products/{productId}/variants/{variantId}/quantity", productId, variantId)
                    .body(Map.of("availableQuantity", quantity))
                    .retrieve()
                    .body(InventoryResponse.class);
            if (response == null || response.availableQuantity() == null) throw new IllegalStateException("Inventory update returned no quantity");
            return response.availableQuantity();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Inventory Service is unavailable", exception);
        }
    }

    private Optional<Integer> fetchAvailableQuantity(String productId) {
        try {
            InventoryResponse response = RetryingRestClient.execute("Fetch inventory for product " + productId, () -> restClient
                    .get()
                    .uri(inventoryServiceBaseUrl + "/api/v1/inventory/products/{productId}", productId)
                    .retrieve()
                    .body(InventoryResponse.class));

            return Optional.ofNullable(response).map(InventoryResponse::availableQuantity);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                log.warn("Inventory not found for productId={}", productId);
                return Optional.empty();
            }
            throw exception;
        }
    }

    private Map<String, Integer> fetchAvailableQuantities(List<String> productIds) {
        List<InventoryResponse> responses = RetryingRestClient.execute("Fetch inventory batch", () -> restClient
                .post()
                .uri(inventoryServiceBaseUrl + "/api/v1/inventory/products/batch")
                .body(new BatchInventoryRequest(productIds))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {}));

        if (responses == null) {
            return Collections.emptyMap();
        }

        return responses.stream()
                .collect(Collectors.toMap(
                        InventoryResponse::productId,
                        InventoryResponse::availableQuantity,
                        (left, right) -> right
                ));
    }

    private InventoryClientException unavailable(String productId, Throwable exception) {
        if (exception instanceof CallNotPermittedException) {
            log.warn("Inventory circuit breaker is open; rejecting request for productId={}", productId);
        } else {
            log.warn("Inventory service is unavailable for productId={}", productId, exception);
        }
        return new InventoryClientException(exception);
    }

    public static class InventoryClientException extends RuntimeException {
        public InventoryClientException(Throwable cause) {
            super("Inventory service is unavailable", cause);
        }
    }
}
