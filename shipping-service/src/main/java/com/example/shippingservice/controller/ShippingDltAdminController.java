package com.example.shippingservice.controller;

import com.example.event.OrderCancelledEvent;
import com.example.event.ShipmentRequestedEvent;
import com.example.shippingservice.messaging.dlt.DltTopicStatus;
import com.example.shippingservice.messaging.dlt.ShippingDltOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Demo-only operational endpoints. Copy a verified payload from Kafka UI before replaying it.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shipments/admin/dlt")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ShippingDltAdminController {

    private final ShippingDltOperations dltOperations;

    @GetMapping
    public List<DltTopicStatus> status() {
        return dltOperations.status();
    }

    @PostMapping("/shipment-requested/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void replayShipmentRequested(@RequestBody ShipmentRequestedEvent event) {
        dltOperations.replayShipmentRequested(event);
    }

    @PostMapping("/order-cancelled/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void replayOrderCancelled(@RequestBody OrderCancelledEvent event) {
        dltOperations.replayOrderCancelled(event);
    }
}
