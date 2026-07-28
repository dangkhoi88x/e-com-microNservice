package com.example.shippingservice.messaging.consumer;

import com.example.event.ShipmentRequestedEvent;
import com.example.shippingservice.dto.request.CreateShipmentRequest;
import com.example.shippingservice.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "SHIPMENT-REQUESTED-CONSUMER")
public class ShipmentRequestedConsumer {

    private final ShipmentService shipmentService;

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(value = 1_000, multiplier = 2),
            retryTopicSuffix = "-retry",
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "shipment-requested", groupId = "shipping-group")
    public void shipmentRequested(ShipmentRequestedEvent event) {
        if (event.getOrderId() == null || event.getUserId() == null || event.getShippingAddress() == null) {
            log.warn("Skip invalid ShipmentRequestedEvent: orderId={}, userId={}",
                    event.getOrderId(), event.getUserId());
            return;
        }

        shipmentService.create(new CreateShipmentRequest(
                event.getOrderId(),
                event.getUserId(),
                event.getShippingAddress()
        ));
        log.info("Handled ShipmentRequestedEvent: orderId={}, orderCode={}",
                event.getOrderId(), event.getOrderCode());
    }
}
