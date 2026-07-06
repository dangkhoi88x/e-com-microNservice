package com.example.notificationservice.messaging.consumer;

import com.example.event.OrderCreatedEvent;
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
}
