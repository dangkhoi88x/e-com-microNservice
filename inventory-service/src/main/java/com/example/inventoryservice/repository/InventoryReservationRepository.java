package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
    List<InventoryReservation> findByOrderId(String orderId);

    List<InventoryReservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);

    boolean existsByOrderId(String orderId);
}
