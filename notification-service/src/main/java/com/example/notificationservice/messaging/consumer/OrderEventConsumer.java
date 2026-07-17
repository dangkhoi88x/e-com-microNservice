package com.example.notificationservice.messaging.consumer;

import com.example.event.OrderCancelledEvent;
import com.example.event.OrderCreatedEvent;
import com.example.event.OrderStatusUpdatedEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-NOTIFICATION-CONSUMER")
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order-created", groupId = "notification-group")
    public void orderCreated(Map<String, Object> payload) {
        OrderCreatedEvent event = toOrderCreatedEvent(payload);
        log.info("Received OrderCreatedEvent: orderId={}, userId={}", event.getOrderId(), event.getUserId());
        notificationService.createNotificationOrderCreated(event);
    }

    @KafkaListener(topics = "order-cancelled", groupId = "notification-group")
    public void orderCancelled(Map<String, Object> payload) {
        OrderCancelledEvent event = toOrderCancelledEvent(payload);
        log.info("Received OrderCancelledEvent: orderId={}, userId={}", event.getOrderId(), event.getUserId());
        notificationService.createNotificationOrderCancelled(event);
    }

    @KafkaListener(topics = "order-status-updated", groupId = "notification-group")
    public void orderStatusUpdated(Map<String, Object> payload) {
        OrderStatusUpdatedEvent event = toOrderStatusUpdatedEvent(payload);
        log.info("Received OrderStatusUpdatedEvent: orderId={}, userId={}, oldStatus={}, newStatus={}",
                event.getOrderId(),
                event.getUserId(),
                event.getOldStatus(),
                event.getNewStatus());
        notificationService.createNotificationOrderStatusUpdated(event);
    }

    private OrderCreatedEvent toOrderCreatedEvent(Map<String, Object> payload) {
        return OrderCreatedEvent.builder()
                .orderId(asString(payload.get("orderId")))
                .orderCode(asString(payload.get("orderCode")))
                .userId(asString(payload.get("userId")))
                .totalAmount(asBigDecimal(payload.get("totalAmount")))
                .status(asString(payload.get("status")))
                .createdAt(asInstant(payload.get("createdAt")))
                .build();
    }

    private OrderCancelledEvent toOrderCancelledEvent(Map<String, Object> payload) {
        return OrderCancelledEvent.builder()
                .orderId(asString(payload.get("orderId")))
                .orderCode(asString(payload.get("orderCode")))
                .userId(asString(payload.get("userId")))
                .totalAmount(asBigDecimal(payload.get("totalAmount")))
                .status(asString(payload.get("status")))
                .cancelledAt(asInstant(payload.get("cancelledAt")))
                .build();
    }

    private OrderStatusUpdatedEvent toOrderStatusUpdatedEvent(Map<String, Object> payload) {
        return OrderStatusUpdatedEvent.builder()
                .orderId(asString(payload.get("orderId")))
                .orderCode(asString(payload.get("orderCode")))
                .userId(asString(payload.get("userId")))
                .oldStatus(asString(payload.get("oldStatus")))
                .newStatus(asString(payload.get("newStatus")))
                .updatedAt(asInstant(payload.get("updatedAt")))
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Instant asInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return Instant.parse(String.valueOf(value));
    }
}
