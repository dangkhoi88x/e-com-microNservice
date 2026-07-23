package com.example.shippingservice.controller;

import com.example.shippingservice.dto.request.AssignCarrierRequest;
import com.example.shippingservice.dto.request.UpdateShipmentStatusRequest;
import com.example.shippingservice.dto.response.ApiResponse;
import com.example.shippingservice.dto.response.ShipmentResponse;
import com.example.shippingservice.entity.ShipmentStatus;
import com.example.shippingservice.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @GetMapping("/my-shipments")
    public ApiResponse<Page<ShipmentResponse>> myShipments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ok("My shipments retrieved successfully",
                shipmentService.getMyShipments(jwt.getSubject(), pageable(page, size)));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<ShipmentResponse> byOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String orderId
    ) {
        return ok("Shipment retrieved successfully",
                shipmentService.getByOrderIdForUser(orderId, jwt.getSubject()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<Page<ShipmentResponse>> all(
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ok("Shipments retrieved successfully", shipmentService.getAll(status, pageable(page, size)));
    }

    @PutMapping("/{shipmentId}/carrier")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ShipmentResponse> assignCarrier(
            @PathVariable UUID shipmentId,
            @RequestBody @Valid AssignCarrierRequest request
    ) {
        return ok("Carrier assigned successfully", shipmentService.assignCarrier(shipmentId, request));
    }

    @PutMapping("/{shipmentId}/packing")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ShipmentResponse> startPacking(
            @PathVariable UUID shipmentId,
            @RequestBody @Valid UpdateShipmentStatusRequest request
    ) {
        return ok("Shipment is being packed", shipmentService.startPacking(shipmentId, request));
    }

    @PutMapping("/{shipmentId}/ready-to-ship")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ShipmentResponse> readyToShip(
            @PathVariable UUID shipmentId,
            @RequestBody @Valid UpdateShipmentStatusRequest request
    ) {
        return ok("Shipment is ready for pickup", shipmentService.readyToShip(shipmentId, request));
    }

    @PutMapping("/{shipmentId}/ship")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ShipmentResponse> ship(
            @PathVariable UUID shipmentId,
            @RequestBody @Valid UpdateShipmentStatusRequest request
    ) {
        return ok("Shipment is in transit", shipmentService.ship(shipmentId, request));
    }

    @PutMapping("/{shipmentId}/deliver")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ShipmentResponse> deliver(
            @PathVariable UUID shipmentId,
            @RequestBody @Valid UpdateShipmentStatusRequest request
    ) {
        return ok("Shipment delivered successfully", shipmentService.deliver(shipmentId, request));
    }

    @PutMapping("/{shipmentId}/delivery-failed")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ShipmentResponse> deliveryFailed(
            @PathVariable UUID shipmentId,
            @RequestBody @Valid UpdateShipmentStatusRequest request
    ) {
        return ok("Delivery failure recorded", shipmentService.markDeliveryFailed(shipmentId, request));
    }

    @PutMapping("/{shipmentId}/returning")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ShipmentResponse> startReturning(
            @PathVariable UUID shipmentId,
            @RequestBody @Valid UpdateShipmentStatusRequest request
    ) {
        return ok("Shipment is returning to sender", shipmentService.startReturning(shipmentId, request));
    }

    @PutMapping("/{shipmentId}/returned")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ShipmentResponse> markReturned(
            @PathVariable UUID shipmentId,
            @RequestBody @Valid UpdateShipmentStatusRequest request
    ) {
        return ok("Shipment returned to sender", shipmentService.markReturned(shipmentId, request));
    }

    @PutMapping("/{shipmentId}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ApiResponse<ShipmentResponse> cancel(
            @PathVariable UUID shipmentId,
            @RequestBody @Valid UpdateShipmentStatusRequest request
    ) {
        return ok("Shipment cancelled successfully", shipmentService.cancel(shipmentId, request));
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build();
    }
}
