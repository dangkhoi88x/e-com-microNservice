package com.example.shippingservice.messaging.consumer;

import com.example.event.OrderCancelledEvent;
import com.example.shippingservice.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-CANCELLED-SHIPPING-CONSUMER")
public class OrderCancelledConsumer {

    private final ShipmentService shipmentService;

    @KafkaListener(topics = "order-cancelled", groupId = "shipping-group")
    public void orderCancelled(OrderCancelledEvent event) {
        if (event.getOrderId() == null || event.getOrderId().isBlank()) {
            log.warn("Skip invalid OrderCancelledEvent without orderId");
            return;
        }
        shipmentService.cancelByOrderId(event.getOrderId());
    }
}
