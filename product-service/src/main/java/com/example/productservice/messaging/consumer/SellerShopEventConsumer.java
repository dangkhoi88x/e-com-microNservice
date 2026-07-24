package com.example.productservice.messaging.consumer;

import com.example.productservice.service.ProductService;
import event.SellerShopStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "SELLER-SHOP-EVENT-CONSUMER")
public class SellerShopEventConsumer {
    private final ProductService productService;

    @KafkaListener(topics = "seller-shop-status-changed", groupId = "product-group")
    public void sellerShopStatusChanged(SellerShopStatusChangedEvent event) {
        if (!"SUSPENDED".equals(event.getStatus())) {
            return;
        }
        log.info("Received shop suspension: shopId={}", event.getShopId());
        productService.inactivateProductsForSuspendedShop(event.getShopId());
    }
}
