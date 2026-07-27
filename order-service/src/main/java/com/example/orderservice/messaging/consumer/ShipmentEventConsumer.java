package com.example.orderservice.messaging.consumer;

import com.example.event.ShipmentStatusUpdatedEvent;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "SHIPMENT-ORDER-CONSUMER")
public class ShipmentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "shipment-status-updated", groupId = "order-group")
    public void shipmentStatusUpdated(ShipmentStatusUpdatedEvent event) {
        log.info("Received ShipmentStatusUpdatedEvent: shipmentId={}, orderId={}, status={}",
                event.getShipmentId(), event.getOrderId(), event.getNewStatus());
        orderService.updateOrderFromShipment(event);
    }
}
