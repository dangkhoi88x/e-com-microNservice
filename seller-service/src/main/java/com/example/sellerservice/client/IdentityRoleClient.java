package com.example.sellerservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IdentityRoleClient {
    @Value("${identity-service.base-url}")
    private String identityServiceBaseUrl;

    public void grantSellerRole(String userId, String authorization) {
        try {
            RestClient.builder()
                    .baseUrl(identityServiceBaseUrl)
                    .build()
                    .post()
                    .uri("/admin/users/{userId}/roles/seller", userId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new IdentityRoleClientException(IdentityRoleClientFailure.REJECTED, exception);
        } catch (ResourceAccessException exception) {
            throw new IdentityRoleClientException(IdentityRoleClientFailure.UNAVAILABLE, exception);
        }
    }

    public enum IdentityRoleClientFailure {
        REJECTED,
        UNAVAILABLE
    }

    @lombok.Getter
    public static class IdentityRoleClientException extends RuntimeException {
        private final IdentityRoleClientFailure failure;

        IdentityRoleClientException(IdentityRoleClientFailure failure, Throwable cause) {
            super(cause);
            this.failure = failure;
        }
    }
}
