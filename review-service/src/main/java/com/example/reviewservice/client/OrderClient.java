package com.example.reviewservice.client;

import com.example.reviewservice.dto.response.ReviewEligibilityResponse;
import com.example.reviewservice.exception.ErrorCode;
import com.example.reviewservice.exception.ReviewServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
public class OrderClient {
    private final WebClient.Builder webClientBuilder;

    @Value("${services.order.base-url:http://ORDER-SERVICE}")
    private String orderServiceBaseUrl;

    public ReviewEligibilityResponse checkEligibility(String orderItemId, String authorization) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(orderServiceBaseUrl + "/internal/orders/items/{orderItemId}/review-eligibility", orderItemId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .bodyToMono(ReviewEligibilityResponse.class)
                    .block();
        } catch (WebClientResponseException.NotFound
                 | WebClientResponseException.Forbidden exception) {
            throw new ReviewServiceException(ErrorCode.REVIEW_NOT_ELIGIBLE);
        } catch (WebClientResponseException | WebClientRequestException exception) {
            throw new ReviewServiceException(ErrorCode.ORDER_SERVICE_UNAVAILABLE);
        }
    }
}
