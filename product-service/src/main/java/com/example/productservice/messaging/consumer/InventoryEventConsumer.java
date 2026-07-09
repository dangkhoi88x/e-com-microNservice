package com.example.productservice.messaging.consumer;

import com.example.productservice.service.ProductService;
import event.InventoryUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "INVENTORY-PRODUCT-CONSUMER")
public class InventoryEventConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "inventory-updated", groupId = "product-group")
    public void inventoryUpdated(InventoryUpdatedEvent event) {
        log.info("Received InventoryUpdatedEvent: productId={}, availableQuantity={}",
                event.getProductId(),
                event.getAvailableQuantity());
        productService.syncStockFromInventoryEvent(event);
    }
}
