package com.example.paymentservice.client;

import com.example.paymentservice.dto.response.ApiResponse;
import com.example.paymentservice.dto.response.OrderResponse;
import com.example.paymentservice.exception.ErrorCode;
import com.example.paymentservice.exception.PaymentServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
public class OrderClient {

    private final WebClient orderWebClient;

    @Value("${services.order.internal-api-key}")
    private String internalApiKey;

    public OrderResponse getOrderDetail(String orderId, String token) {
        return getOrderDetail("/api/v1/orders/{id}", orderId, token);
    }

    /**
     * Used only by an admin-confirmed payment result (for example COD).
     * The order service enforces the ADMIN authority on this endpoint.
     */
    public OrderResponse getOrderDetailForAdmin(String orderId, String token) {
        return getOrderDetail("/api/v1/orders/{id}/admin", orderId, token);
    }

    public OrderResponse getOrderDetailForPaymentWebhook(String orderId) {
        try {
            ApiResponse<OrderResponse> response = orderWebClient.get()
                    .uri("/internal/orders/{id}/payment-validation", orderId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<OrderResponse>>() {
                    })
                    .block();

            return response != null ? response.getData() : null;
        } catch (WebClientResponseException.NotFound exception) {
            throw new PaymentServiceException(ErrorCode.ORDER_NOT_FOUND);
        } catch (WebClientResponseException.Forbidden exception) {
            throw new PaymentServiceException(ErrorCode.ORDER_ACCESS_DENIED);
        } catch (WebClientException exception) {
            throw new PaymentServiceException(ErrorCode.ORDER_SERVICE_UNAVAILABLE);
        }
    }

    private OrderResponse getOrderDetail(String uri, String orderId, String token) {
        try {
            ApiResponse<OrderResponse> response = orderWebClient.get()
                    .uri(uri, orderId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<OrderResponse>>() {
                    })
                    .block();

            return response != null ? response.getData() : null;
        } catch (WebClientResponseException.NotFound exception) {
            throw new PaymentServiceException(ErrorCode.ORDER_NOT_FOUND);
        } catch (WebClientResponseException.Forbidden exception) {
            throw new PaymentServiceException(ErrorCode.ORDER_ACCESS_DENIED);
        } catch (WebClientException exception) {
            throw new PaymentServiceException(ErrorCode.ORDER_SERVICE_UNAVAILABLE);
        }
    }
}
