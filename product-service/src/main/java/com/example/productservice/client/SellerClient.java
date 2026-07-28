package com.example.productservice.client;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j(topic = "PRODUCT-SELLER-CLIENT")
public class SellerClient {
    private final RestClient restClient;
    private final String sellerServiceBaseUrl;
    private final CircuitBreaker sellerCircuitBreaker;

    public SellerClient(
            @Value("${seller-service.base-url:http://localhost:8098}") String sellerServiceBaseUrl,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.restClient = RestClient.builder().build();
        this.sellerServiceBaseUrl = sellerServiceBaseUrl;
        this.sellerCircuitBreaker = circuitBreakerRegistry.circuitBreaker("sellerService");
    }

    public SellerEligibility requireApprovedShop(String authorization) {
        try {
            SellerEligibility eligibility = sellerCircuitBreaker.executeSupplier(
                    () -> fetchApprovedShop(authorization)
            );
            if (eligibility == null) {
                throw new SellerClientException(SellerClientFailure.NOT_ELIGIBLE);
            }
            return eligibility;
        } catch (CallNotPermittedException exception) {
            log.warn("Seller circuit breaker is open; rejecting eligibility check");
            throw new SellerClientException(SellerClientFailure.UNAVAILABLE);
        } catch (RestClientException exception) {
            log.warn("Seller-service eligibility check failed", exception);
            throw new SellerClientException(SellerClientFailure.UNAVAILABLE);
        }
    }

    private SellerEligibility fetchApprovedShop(String authorization) {
        try {
            SellerEligibilityEnvelope response = RetryingRestClient.execute("Check seller eligibility", () -> restClient.get()
                    .uri(sellerServiceBaseUrl + "/api/v1/sellers/me/eligibility")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(SellerEligibilityEnvelope.class));

            if (response == null || response.data() == null || !response.data().approved()
                    || response.data().shopId() == null) {
                return null;
            }
            return response.data();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                return null;
            }
            throw exception;
        }
    }

    public record SellerEligibilityEnvelope(int status, String message, SellerEligibility data) {
    }

    public record SellerEligibility(boolean approved, String shopId, String status) {
    }

    public enum SellerClientFailure {
        NOT_ELIGIBLE,
        UNAVAILABLE
    }

    public static class SellerClientException extends RuntimeException {
        private final SellerClientFailure failure;

        public SellerClientException(SellerClientFailure failure) {
            this.failure = failure;
        }

        public SellerClientFailure getFailure() {
            return failure;
        }
    }
}
