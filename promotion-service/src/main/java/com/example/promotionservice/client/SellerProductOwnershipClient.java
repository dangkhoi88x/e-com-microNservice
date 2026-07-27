package com.example.promotionservice.client;

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

    public SellerProductOwnershipClient(@Value("${product-service.base-url:http://localhost:8084}") String productServiceBaseUrl) {
        this.productServiceBaseUrl = productServiceBaseUrl;
    }

    public boolean ownsAll(String sellerId, List<String> productIds) {
        try {
            OwnershipResponse response = restClient.get()
                    .uri(productServiceBaseUrl + "/internal/products/ownership?sellerId={sellerId}&productIds={productIds}", sellerId, String.join(",", productIds))
                    .retrieve()
                    .body(OwnershipResponse.class);
            return response != null && response.owned();
        } catch (RestClientException exception) {
            log.error("Cannot verify Seller product ownership", exception);
            return false;
        }
    }

    private record OwnershipResponse(boolean owned) { }
}
