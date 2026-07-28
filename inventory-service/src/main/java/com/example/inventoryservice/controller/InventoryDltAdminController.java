package com.example.inventoryservice.controller;

import com.example.event.PaymentCancelledEvent;
import com.example.event.PaymentFailedEvent;
import com.example.event.PaymentSuccessEvent;
import com.example.inventoryservice.messaging.dlt.DltTopicStatus;
import com.example.inventoryservice.messaging.dlt.InventoryDltOperations;
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
@RequestMapping("/api/v1/inventory/admin/dlt")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class InventoryDltAdminController {

    private final InventoryDltOperations dltOperations;

    @GetMapping
    public List<DltTopicStatus> status() {
        return dltOperations.status();
    }

    @PostMapping("/payment-success/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void replayPaymentSuccess(@RequestBody PaymentSuccessEvent event) {
        dltOperations.replayPaymentSuccess(event);
    }

    @PostMapping("/payment-failed/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void replayPaymentFailed(@RequestBody PaymentFailedEvent event) {
        dltOperations.replayPaymentFailed(event);
    }

    @PostMapping("/payment-cancelled/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void replayPaymentCancelled(@RequestBody PaymentCancelledEvent event) {
        dltOperations.replayPaymentCancelled(event);
    }
}
