package com.example.orderservice.messaging.consumer;

import com.example.event.PaymentSuccessEvent;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-ORDER-CONSUMER")
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-success", groupId = "order-group")
    public void paymentSuccess(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent: paymentId={}, orderId={}, userId={}",
                event.getPaymentId(),
                event.getOrderId(),
                event.getUserId());
        orderService.confirmOrderFromPaymentSuccess(event);
    }
}
