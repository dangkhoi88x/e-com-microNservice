package com.example.notificationservice.messaging.consumer;

import com.example.event.UserCreatedEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "USER-CREATED-NOTIFICATION-CONSUMER")

public class UserCreatedConsumer {
    private final NotificationService notificationService;

    @KafkaListener(topics = "created-user-topic", groupId = "notification-group")
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for notification: userId={}", event.getUserId());
        notificationService.createNotificationWelcome(event);
    }
}
