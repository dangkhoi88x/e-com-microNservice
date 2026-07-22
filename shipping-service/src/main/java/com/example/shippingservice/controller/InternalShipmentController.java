package com.example.shippingservice.controller;

import com.example.shippingservice.dto.request.CreateShipmentRequest;
import com.example.shippingservice.dto.response.ApiResponse;
import com.example.shippingservice.dto.response.ShipmentResponse;
import com.example.shippingservice.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/shipments")
public class InternalShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ApiResponse<ShipmentResponse> create(@RequestBody @Valid CreateShipmentRequest request) {
        return ApiResponse.<ShipmentResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Shipment created or already exists")
                .data(shipmentService.create(request))
                .build();
    }
}
