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
//DLT giữ message lỗi để admin kiểm tra và replay.
    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(value = 1_000, multiplier = 2),
            retryTopicSuffix = "-retry",
            dltTopicSuffix = ".DLT"
    )
    //Order Service phát event: xử lý event đó
    @KafkaListener(topics = "shipment-requested", groupId = "shipping-group")
    public void shipmentRequested(ShipmentRequestedEvent event) {
        if (event.getOrderId() == null || event.getUserId() == null || event.getShippingAddress() == null) {
            log.warn("Skip invalid ShipmentRequestedEvent: orderId={}, userId={}",
                    event.getOrderId(), event.getUserId());
            return;
        }
    //Chuyển event thành request
        //Consumer chỉ nhận message và chuyển dữ liệu. Nghiệp vụ tạo shipment nằm trong service.
        shipmentService.create(new CreateShipmentRequest(
                event.getOrderId(),
                event.getUserId(),
                event.getShippingAddress()
        ));
        log.info("Handled ShipmentRequestedEvent: orderId={}, orderCode={}",
                event.getOrderId(), event.getOrderCode());
    }
}
