package com.example.shippingservice.repository;

import com.example.shippingservice.entity.ShipmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentHistoryRepository extends JpaRepository<ShipmentHistory, UUID> {
    List<ShipmentHistory> findAllByShipmentIdOrderByOccurredAtAsc(UUID shipmentId);
}
