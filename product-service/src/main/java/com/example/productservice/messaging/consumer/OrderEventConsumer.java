package com.example.productservice.messaging.consumer;

import com.example.productservice.service.ProductService;
import com.example.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-PRODUCT-CONSUMER")
public class OrderEventConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "order-created", groupId = "product-group")
    public void orderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}", event.getOrderId());
        productService.reduceStockFromOrderCreatedEvent(event);
    }
}
