package com.example.promotionservice.repository;

import com.example.promotionservice.entity.FlashDealReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface FlashDealReservationRepository extends JpaRepository<FlashDealReservation, UUID> {
    List<FlashDealReservation> findAllByOrderId(String orderId);
}
