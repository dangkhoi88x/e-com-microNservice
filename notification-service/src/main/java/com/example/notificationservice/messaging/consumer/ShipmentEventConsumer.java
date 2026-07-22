package com.example.notificationservice.messaging.consumer;

import com.example.event.ShipmentStatusUpdatedEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "SHIPMENT-NOTIFICATION-CONSUMER")
public class ShipmentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "shipment-status-updated", groupId = "notification-group")
    public void shipmentStatusUpdated(Map<String, Object> payload) {
        ShipmentStatusUpdatedEvent event = ShipmentStatusUpdatedEvent.builder()
                .shipmentId(asUuid(payload.get("shipmentId")))
                .orderId(asString(payload.get("orderId")))
                .userId(asString(payload.get("userId")))
                .oldStatus(asString(payload.get("oldStatus")))
                .newStatus(asString(payload.get("newStatus")))
                .carrier(asString(payload.get("carrier")))
                .trackingNumber(asString(payload.get("trackingNumber")))
                .description(asString(payload.get("description")))
                .location(asString(payload.get("location")))
                .updatedAt(asInstant(payload.get("updatedAt")))
                .build();

        if (event.getOrderId() == null || event.getUserId() == null || event.getNewStatus() == null) {
            log.warn("Skip invalid shipment notification payload: {}", payload);
            return;
        }
        notificationService.createNotificationShipmentStatusUpdated(event);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private UUID asUuid(Object value) {
        return value == null ? null : UUID.fromString(String.valueOf(value));
    }

    private Instant asInstant(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Instant instant ? instant : Instant.parse(String.valueOf(value));
    }
}
