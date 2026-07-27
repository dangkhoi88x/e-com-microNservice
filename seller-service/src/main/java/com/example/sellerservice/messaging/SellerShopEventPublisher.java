package com.example.sellerservice.messaging;

import event.SellerShopStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "SELLER-SHOP-EVENT-PUBLISHER")
public class SellerShopEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishStatusChanged(SellerShopStatusChangedEvent event) {
        kafkaTemplate.send("seller-shop-status-changed", event.getShopId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish seller shop status change: shopId={}", event.getShopId(), throwable);
                        return;
                    }
                    log.info("Published seller shop status change: shopId={}, status={}",
                            event.getShopId(), event.getStatus());
                });
    }
}
