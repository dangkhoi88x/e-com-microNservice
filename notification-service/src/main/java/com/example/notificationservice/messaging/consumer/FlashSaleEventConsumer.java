package com.example.notificationservice.messaging.consumer;

import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component @RequiredArgsConstructor @Slf4j(topic = "FLASH-SALE-NOTIFICATION-CONSUMER")
public class FlashSaleEventConsumer {
    private final NotificationService notificationService;
    @KafkaListener(topics = "flash-sale-upcoming", groupId = "notification-group")
    public void flashSaleUpcoming(Map<String, Object> payload) {
        String userId = value(payload, "userId");
        String name = value(payload, "flashDealName");
        String startAt = value(payload, "startAt");
        if (userId == null || name == null || startAt == null) { log.warn("Skip invalid Flash Sale notification payload: {}", payload); return; }
        notificationService.createNotificationFlashSaleUpcoming(userId, name, startAt);
    }
    private String value(Map<String, Object> payload, String key) { Object value = payload.get(key); return value == null ? null : String.valueOf(value); }
}
