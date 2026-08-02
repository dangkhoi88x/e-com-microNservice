package com.example.promotionservice.client;

import com.example.promotionservice.exception.ErrorCode;
import com.example.promotionservice.exception.PromotionServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@Slf4j
public class SellerProductOwnershipClient {
    private final RestClient restClient = RestClient.builder().build();
    private final String productServiceBaseUrl;
    private final CircuitBreaker productServiceCircuitBreaker;

    public SellerProductOwnershipClient(
            @Value("${product-service.base-url:http://localhost:8084}") String productServiceBaseUrl,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.productServiceBaseUrl = productServiceBaseUrl;
        this.productServiceCircuitBreaker = circuitBreakerRegistry.circuitBreaker("productService");
    }

    public boolean ownsAll(String sellerId, List<String> productIds) {
        try {
            return productServiceCircuitBreaker.executeSupplier(() -> fetchOwnership(sellerId, productIds)).owned();
        } catch (CallNotPermittedException | RestClientException | ProductOwnershipUnavailableException exception) {
            log.error("Cannot verify Seller product ownership", exception);
            throw new PromotionServiceException(ErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
        }
    }

    private OwnershipResponse fetchOwnership(String sellerId, List<String> productIds) {
        OwnershipResponse response = RetryingRestClient.execute("Verify seller product ownership", () -> restClient.get()
                .uri(productServiceBaseUrl + "/internal/products/ownership?sellerId={sellerId}&productIds={productIds}", sellerId, String.join(",", productIds))
                .retrieve()
                .body(OwnershipResponse.class));
        if (response == null) {
            throw new ProductOwnershipUnavailableException();
        }
        return response;
    }

    private record OwnershipResponse(boolean owned) { }

    private static class ProductOwnershipUnavailableException extends RuntimeException {
    }
}
