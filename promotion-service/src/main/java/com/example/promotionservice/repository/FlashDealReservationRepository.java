package com.example.promotionservice.repository;

import com.example.promotionservice.entity.FlashDealReservation;
import com.example.promotionservice.entity.FlashDealReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface FlashDealReservationRepository extends JpaRepository<FlashDealReservation, UUID> {
    List<FlashDealReservation> findAllByOrderId(String orderId);

    @Query("select r.flashDealItem.id as itemId, count(distinct r.orderId) as orderCount, coalesce(sum(r.quantity), 0) as soldQuantity, coalesce(sum(r.salePrice * r.quantity), 0) as revenue from FlashDealReservation r where r.flashDealItem.flashDeal.id = :dealId and r.status = :status group by r.flashDealItem.id")
    List<FlashDealItemMetricProjection> summarizeConfirmedByItem(@Param("dealId") UUID dealId, @Param("status") FlashDealReservationStatus status);

    @Query("select count(distinct r.orderId) as orderCount, coalesce(sum(r.quantity), 0) as soldQuantity, coalesce(sum(r.salePrice * r.quantity), 0) as revenue from FlashDealReservation r where r.flashDealItem.flashDeal.id = :dealId and r.status = :status")
    FlashDealCampaignMetricProjection summarizeCampaign(@Param("dealId") UUID dealId, @Param("status") FlashDealReservationStatus status);
}
