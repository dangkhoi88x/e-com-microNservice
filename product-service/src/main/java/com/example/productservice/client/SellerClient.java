package com.example.productservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j(topic = "PRODUCT-SELLER-CLIENT")
public class SellerClient {
    private final RestClient restClient;
    private final String sellerServiceBaseUrl;

    public SellerClient(@Value("${seller-service.base-url:http://localhost:8098}") String sellerServiceBaseUrl) {
        this.restClient = RestClient.builder().build();
        this.sellerServiceBaseUrl = sellerServiceBaseUrl;
    }

    public SellerEligibility requireApprovedShop(String authorization) {
        try {
            SellerEligibilityEnvelope response = restClient.get()
                    .uri(sellerServiceBaseUrl + "/api/v1/sellers/me/eligibility")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(SellerEligibilityEnvelope.class);

            if (response == null || response.data() == null || !response.data().approved()
                    || response.data().shopId() == null) {
                throw new SellerClientException(SellerClientFailure.NOT_ELIGIBLE);
            }
            return response.data();
        } catch (SellerClientException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Seller-service eligibility check failed", exception);
            throw new SellerClientException(SellerClientFailure.UNAVAILABLE);
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
