package com.example.inventoryservice.messaging.consumer;

import com.example.event.PaymentCancelledEvent;
import com.example.event.PaymentFailedEvent;
import com.example.event.PaymentSuccessEvent;
import com.example.inventoryservice.dto.request.InventoryOrderRequest;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-INVENTORY-CONSUMER")
public class PaymentEventConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(topics = "payment-success", groupId = "inventory-group")
    public void paymentSuccess(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent: paymentId={}, orderId={}",
                event.getPaymentId(),
                event.getOrderId());
        inventoryService.confirmInventory(new InventoryOrderRequest(event.getOrderId()));
    }

    @KafkaListener(topics = "payment-failed", groupId = "inventory-group")
    public void paymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: paymentId={}, orderId={}",
                event.getPaymentId(),
                event.getOrderId());
        inventoryService.releaseInventory(new InventoryOrderRequest(event.getOrderId()));
    }

    @KafkaListener(topics = "payment-cancelled", groupId = "inventory-group")
    public void paymentCancelled(PaymentCancelledEvent event) {
        log.info("Received PaymentCancelledEvent: paymentId={}, orderId={}",
                event.getPaymentId(),
                event.getOrderId());
        inventoryService.releaseInventory(new InventoryOrderRequest(event.getOrderId()));
    }
}
