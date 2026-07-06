package com.example.orderservice.client;

import com.example.orderservice.dto.response.ApiResponse;
import com.example.orderservice.dto.response.ProductDetailResponse;
import com.example.orderservice.exception.ErrorCode;
import com.example.orderservice.exception.OrderServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RequiredArgsConstructor
@Component
public class ProductClient {

    private final WebClient productWebClient;

    public ProductDetailResponse getProductById(String productId) {
        try {
            ApiResponse<ProductDetailResponse> response = productWebClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponse<ProductDetailResponse>>() {})
                    .block();

            return response != null ? response.getData() : null;
        } catch (WebClientResponseException.NotFound exception) {
            throw new OrderServiceException(ErrorCode.PRODUCT_NOT_FOUND);
        } catch (WebClientException exception) {
            throw new OrderServiceException(ErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
        }
    }
}
