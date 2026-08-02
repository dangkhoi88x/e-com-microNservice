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
    private static final String CIRCUIT_BREAKER_NAME = "sellerService";
    private static final String ELIGIBILITY_PATH = "/api/v1/sellers/me/eligibility";   /// để kiểm tra shop
    private static final String ELIGIBILITY_OPERATION = "Check seller eligibility";
    // HTTP client dùng để gửi request sang Seller Service
    private final RestClient restClient;
    private final String sellerServiceBaseUrl;
    private final CircuitBreaker sellerCircuitBreaker;

    public SellerClient(
            @Value("${seller-service.base-url:http://localhost:8098}") String sellerServiceBaseUrl,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        // // Tạo HTTP client riêng cho Seller Service.
        this.restClient = RestClient.builder().build();
        this.sellerServiceBaseUrl = sellerServiceBaseUrl;
        this.sellerCircuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    }
    //Kiểm tra user hiện tại có shop được duyệt hay không.
    public SellerEligibility requireApprovedShop(String authorization) {
        try {
            SellerEligibility eligibility = sellerCircuitBreaker.executeSupplier(
                    () -> requestSellerEligibility(authorization)
            );
             // Kiểm tra response có thật sự là shop đã duyệt không.
            return requireApprovedEligibility(eligibility);
        } catch (CallNotPermittedException exception) {
            log.warn("Seller circuit breaker is open; rejecting eligibility check");
            throw sellerServiceUnavailable(exception);
        } catch (RestClientException exception) {
            log.warn("Seller-service eligibility check failed", exception);
            throw sellerServiceUnavailable(exception);
        }
    }
    //Gọi Seller Service và lấy phần data trong response
    private SellerEligibility requestSellerEligibility(String authorization) {
        try {
             // Gửi HTTP request và nhận response đầy đủ.
            SellerEligibilityEnvelope response = sendEligibilityRequest(authorization);
            return response == null ? null : response.data();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                return null;
            }
            throw exception;
        }
    }
        //Chỉ chịu trách nhiệm tạo và gửi HTTP request
    private SellerEligibilityEnvelope sendEligibilityRequest(String authorization) {
        return RetryingRestClient.execute(ELIGIBILITY_OPERATION, () -> restClient.get()
                .uri(sellerServiceBaseUrl + ELIGIBILITY_PATH)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(SellerEligibilityEnvelope.class));
    }

    private SellerEligibility requireApprovedEligibility(SellerEligibility eligibility) {
        if (eligibility == null || !eligibility.approved() || eligibility.shopId() == null) {
            throw new SellerClientException(SellerClientFailure.NOT_ELIGIBLE);
        }
        return eligibility;
    }

    private SellerClientException sellerServiceUnavailable(Throwable cause) {
        return new SellerClientException(SellerClientFailure.UNAVAILABLE, cause);
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
            this(failure, null);
        }

        public SellerClientException(SellerClientFailure failure, Throwable cause) {
            super(failure.name(), cause);
            this.failure = failure;
        }

        public SellerClientFailure getFailure() {
            return failure;
        }
    }
}
