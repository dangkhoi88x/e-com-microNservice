package com.example.orderservice.client;

import com.example.orderservice.dto.response.PromotionCalculationResponse;
import com.example.orderservice.configuration.TraceIdWebClientFilter;
import com.example.orderservice.exception.ErrorCode;
import com.example.orderservice.exception.OrderServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.example.orderservice.dto.response.FlashDealPriceResponse;

@Component
@RequiredArgsConstructor
public class PromotionClient {
    @Value("${promotion-service.base-url:http://PROMOTION-SERVICE}")
    private String promotionServiceBaseUrl;

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

    public List<FlashDealPriceResponse> reserveFlashDeals(String orderId, List<FlashDealItemRequest> items) {
        try {
            ApiResponse<List<FlashDealPriceResponse>> response = client().post().uri(flashBaseUrl() + "/reserve")
                    .bodyValue(new FlashDealReserveRequest(orderId, items)).retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<FlashDealPriceResponse>>>() {}).block();
            return response == null || response.data() == null ? List.of() : response.data();
        } catch (WebClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) throw new OrderServiceException(ErrorCode.FLASH_SALE_RESERVATION_FAILED);
            throw new OrderServiceException(ErrorCode.PROMOTION_SERVICE_UNAVAILABLE);
        } catch (WebClientException exception) { throw new OrderServiceException(ErrorCode.PROMOTION_SERVICE_UNAVAILABLE); }
    }
    public void confirmFlashDeals(String orderId) { postFlashWithoutResponse("/confirm", new OrderRequest(orderId)); }
    public void releaseFlashDeals(String orderId) { postFlashWithoutResponse("/release", new OrderRequest(orderId)); }

    private PromotionCalculationResponse postForCalculation(String path, Object body, ErrorCode clientError) {
        try {
            ApiResponse<PromotionCalculationResponse> response = client().post()
                    .uri(promotionBaseUrl() + path)
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
            client().post().uri(promotionBaseUrl() + path).bodyValue(body).retrieve().toBodilessEntity().block();
        } catch (WebClientException exception) {
            throw new OrderServiceException(ErrorCode.PROMOTION_SERVICE_UNAVAILABLE);
        }
    }
    private void postFlashWithoutResponse(String path, Object body) {
        try { client().post().uri(flashBaseUrl() + path).bodyValue(body).retrieve().toBodilessEntity().block(); }
        catch (WebClientResponseException exception) { throw new OrderServiceException(ErrorCode.PROMOTION_SERVICE_UNAVAILABLE); }
        catch (WebClientException exception) { throw new OrderServiceException(ErrorCode.PROMOTION_SERVICE_UNAVAILABLE); }
    }

    private record ApiResponse<T>(T data) {}

    private String promotionBaseUrl() {
        return promotionServiceBaseUrl + "/internal/promotions";
    }

    private String flashBaseUrl() {
        return promotionServiceBaseUrl + "/internal/flash-deals";
    }

    private WebClient client() {
        return promotionServiceBaseUrl.startsWith("http://localhost")
                ? WebClient.builder().filter(TraceIdWebClientFilter.propagateTraceId()).build()
                : webClientBuilder.build();
    }

    private record ValidateRequest(String campaignCode, BigDecimal subtotalAmount) {}
    private record ReserveRequest(String campaignCode, String userId, String orderId, BigDecimal subtotalAmount) {}
    private record OrderRequest(String orderId) {}
    public record FlashDealItemRequest(String productId, String variantId, Integer quantity) {}
    private record FlashDealReserveRequest(String orderId, List<FlashDealItemRequest> items) {}
}
