package com.example.orderservice.client;

import com.example.orderservice.dto.response.PromotionCalculationResponse;
import com.example.orderservice.exception.ErrorCode;
import com.example.orderservice.exception.OrderServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PromotionClient {
    private static final String BASE_URL = "http://PROMOTION-SERVICE/internal/promotions";
    private final WebClient.Builder webClientBuilder;

    public PromotionCalculationResponse validate(String campaignCode, BigDecimal subtotalAmount) {
        return postForCalculation("/validate", new ValidateRequest(campaignCode, subtotalAmount), ErrorCode.PROMOTION_NOT_APPLICABLE);
    }

    public PromotionCalculationResponse reserve(String campaignCode, String userId, String orderId, BigDecimal subtotalAmount) {
        return postForCalculation("/reserve", new ReserveRequest(campaignCode, userId, orderId, subtotalAmount), ErrorCode.PROMOTION_RESERVATION_FAILED);
    }

    public void confirm(String orderId) {
        postWithoutResponse("/confirm", new OrderRequest(orderId));
    }

    public void release(String orderId) {
        postWithoutResponse("/release", new OrderRequest(orderId));
    }

    private PromotionCalculationResponse postForCalculation(String path, Object body, ErrorCode clientError) {
        try {
            ApiResponse<PromotionCalculationResponse> response = webClientBuilder.build().post()
                    .uri(BASE_URL + path)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<PromotionCalculationResponse>>() {})
                    .block();
            if (response == null || response.data() == null || !response.data().eligible()) {
                throw new OrderServiceException(clientError);
            }
            return response.data();
        } catch (WebClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) throw new OrderServiceException(clientError);
            throw new OrderServiceException(ErrorCode.PROMOTION_SERVICE_UNAVAILABLE);
        } catch (WebClientException exception) {
            throw new OrderServiceException(ErrorCode.PROMOTION_SERVICE_UNAVAILABLE);
        }
    }

    private void postWithoutResponse(String path, Object body) {
        try {
            webClientBuilder.build().post().uri(BASE_URL + path).bodyValue(body).retrieve().toBodilessEntity().block();
        } catch (WebClientException exception) {
            throw new OrderServiceException(ErrorCode.PROMOTION_SERVICE_UNAVAILABLE);
        }
    }

    private record ApiResponse<T>(T data) {}
    private record ValidateRequest(String campaignCode, BigDecimal subtotalAmount) {}
    private record ReserveRequest(String campaignCode, String userId, String orderId, BigDecimal subtotalAmount) {}
    private record OrderRequest(String orderId) {}
}
