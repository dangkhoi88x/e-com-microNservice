package com.example.shippingservice.controller;

import com.example.shippingservice.dto.response.ShipmentResponse;
import com.example.shippingservice.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/shipments")
public class InternalShipmentController {
    private final ShipmentService shipmentService;
    @GetMapping("/orders/{orderId}") public ShipmentResponse byOrder(@PathVariable String orderId) { return shipmentService.getByOrderId(orderId); }
}
