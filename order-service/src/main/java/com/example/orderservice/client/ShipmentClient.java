package com.example.orderservice.client;

import com.example.orderservice.dto.response.SellerOrderDetailResponse;
import com.example.orderservice.configuration.TraceIdFilter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ShipmentClient {
    private final RestClient client = RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
                if (traceId != null && !traceId.isBlank()) {
                    request.getHeaders().set(TraceIdFilter.TRACE_ID_HEADER, traceId);
                }
                return execution.execute(request, body);
            })
            .build();
    private final String baseUrl;
    public ShipmentClient(@Value("${shipping-service.base-url:http://localhost:8096}") String baseUrl) { this.baseUrl = baseUrl; }
    public SellerOrderDetailResponse.Shipment getByOrderId(String orderId) {
        try {
            ShipmentResponse value = client.get().uri(baseUrl + "/internal/shipments/orders/{orderId}", orderId).retrieve().body(ShipmentResponse.class);
            if (value == null) return null;
            return new SellerOrderDetailResponse.Shipment(value.id().toString(), value.carrier(), value.trackingNumber(), value.status(), value.estimatedDeliveryAt(), value.shippedAt(), value.deliveredAt(), value.timeline() == null ? List.of() : value.timeline().stream().map(item -> new SellerOrderDetailResponse.TimelineItem(item.status(), item.description(), item.occurredAt())).toList());
        } catch (RestClientException exception) { return null; }
    }
    private record ShipmentResponse(UUID id, String carrier, String trackingNumber, String status, Instant estimatedDeliveryAt, Instant shippedAt, Instant deliveredAt, List<Timeline> timeline) { }
    private record Timeline(String status, String description, Instant occurredAt) { }
}
