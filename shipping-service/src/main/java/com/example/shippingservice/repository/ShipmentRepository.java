package com.example.shippingservice.repository;

import com.example.shippingservice.entity.Shipment;
import com.example.shippingservice.entity.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByOrderId(String orderId);

    Optional<Shipment> findByOrderIdAndUserId(String orderId, String userId);

    boolean existsByOrderId(String orderId);

    boolean existsByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumberAndIdNot(String trackingNumber, UUID id);

    Page<Shipment> findAllByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Shipment> findAllByStatusOrderByCreatedAtDesc(ShipmentStatus status, Pageable pageable);

    Page<Shipment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
