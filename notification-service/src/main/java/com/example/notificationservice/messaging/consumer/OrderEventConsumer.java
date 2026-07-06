package com.example.notificationservice.messaging.consumer;

import com.example.event.OrderCancelledEvent;
import com.example.event.OrderCreatedEvent;
import com.example.event.OrderStatusUpdatedEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-NOTIFICATION-CONSUMER")
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order-created", groupId = "notification-group")
    public void orderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, userId={}", event.getOrderId(), event.getUserId());
        notificationService.createNotificationOrderCreated(event);
    }

    @KafkaListener(topics = "order-cancelled", groupId = "notification-group")
    public void orderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: orderId={}, userId={}", event.getOrderId(), event.getUserId());
        notificationService.createNotificationOrderCancelled(event);
    }

    @KafkaListener(topics = "order-status-updated", groupId = "notification-group")
    public void orderStatusUpdated(OrderStatusUpdatedEvent event) {
        log.info("Received OrderStatusUpdatedEvent: orderId={}, userId={}, oldStatus={}, newStatus={}",
                event.getOrderId(),
                event.getUserId(),
                event.getOldStatus(),
                event.getNewStatus());
        notificationService.createNotificationOrderStatusUpdated(event);
    }
}
