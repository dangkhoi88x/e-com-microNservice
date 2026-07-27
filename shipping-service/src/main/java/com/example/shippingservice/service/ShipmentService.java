package com.example.shippingservice.service;

import com.example.shippingservice.dto.request.AssignCarrierRequest;
import com.example.shippingservice.dto.request.CreateShipmentRequest;
import com.example.shippingservice.dto.request.UpdateShipmentStatusRequest;
import com.example.shippingservice.dto.response.ShipmentResponse;
import com.example.shippingservice.entity.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ShipmentService {
    ShipmentResponse create(CreateShipmentRequest request);

    ShipmentResponse getByOrderId(String orderId);

    ShipmentResponse getByOrderIdForUser(String orderId, String userId);

    Page<ShipmentResponse> getMyShipments(String userId, Pageable pageable);

    Page<ShipmentResponse> getAll(ShipmentStatus status, Pageable pageable);

    ShipmentResponse assignCarrier(UUID shipmentId, AssignCarrierRequest request);

    ShipmentResponse startPacking(UUID shipmentId, UpdateShipmentStatusRequest request);

    ShipmentResponse readyToShip(UUID shipmentId, UpdateShipmentStatusRequest request);

    ShipmentResponse ship(UUID shipmentId, UpdateShipmentStatusRequest request);

    ShipmentResponse deliver(UUID shipmentId, UpdateShipmentStatusRequest request);

    ShipmentResponse markDeliveryFailed(UUID shipmentId, UpdateShipmentStatusRequest request);

    ShipmentResponse startReturning(UUID shipmentId, UpdateShipmentStatusRequest request);

    ShipmentResponse markReturned(UUID shipmentId, UpdateShipmentStatusRequest request);

    ShipmentResponse cancel(UUID shipmentId, UpdateShipmentStatusRequest request);

    void cancelByOrderId(String orderId);
}
