package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
    List<InventoryReservation> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from InventoryReservation reservation where reservation.orderId = :orderId")
    List<InventoryReservation> findByOrderIdForUpdate(@Param("orderId") String orderId);

    List<InventoryReservation> findByOrderIdAndStatus(String orderId, ReservationStatus status);

    boolean existsByOrderId(String orderId);
}
