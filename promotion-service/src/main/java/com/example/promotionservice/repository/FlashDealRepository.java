package com.example.promotionservice.repository;

import com.example.promotionservice.entity.FlashDeal;
import com.example.promotionservice.entity.FlashDealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FlashDealRepository extends JpaRepository<FlashDeal, UUID> {
    List<FlashDeal> findAllByOrderByStartAtDesc();
    List<FlashDeal> findAllBySellerIdOrderByStartAtDesc(String sellerId);
    List<FlashDeal> findAllByStatusOrderByStartAtAsc(FlashDealStatus status);
    List<FlashDeal> findAllByStatusAndStartAtAfterAndStartAtLessThanEqual(FlashDealStatus status, Instant after, Instant before);

    @Modifying
    @Query("update FlashDeal deal set deal.status = com.example.promotionservice.entity.FlashDealStatus.ENDED where deal.status in (com.example.promotionservice.entity.FlashDealStatus.SCHEDULED, com.example.promotionservice.entity.FlashDealStatus.LIVE) and deal.endAt <= :now")
    int markEnded(@Param("now") Instant now);

    @Modifying
    @Query("update FlashDeal deal set deal.status = com.example.promotionservice.entity.FlashDealStatus.LIVE where deal.status = com.example.promotionservice.entity.FlashDealStatus.SCHEDULED and deal.startAt <= :now and deal.endAt > :now")
    int markLive(@Param("now") Instant now);

    @Modifying
    @Query(value = "update flash_deals deal set status = 'SOLD_OUT' where deal.id = :dealId and not exists (select 1 from flash_deal_items item where item.flash_deal_id = deal.id and (coalesce(item.quota_limited, true) = false or item.quota > 0))", nativeQuery = true)
    int markSoldOut(@Param("dealId") UUID dealId);
}
