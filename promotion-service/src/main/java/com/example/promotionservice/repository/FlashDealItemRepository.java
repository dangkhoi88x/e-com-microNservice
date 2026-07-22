package com.example.promotionservice.repository;

import com.example.promotionservice.entity.FlashDealItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface FlashDealItemRepository extends JpaRepository<FlashDealItem, UUID> {
    @Query("select item from FlashDealItem item join fetch item.flashDeal deal where item.productId = :productId and (item.variantId = :variantId or (item.variantId is null and :variantId is null)) and deal.status = com.example.promotionservice.entity.FlashDealStatus.LIVE and deal.startAt <= :now and deal.endAt > :now order by item.salePrice asc, case when deal.saleType is null or deal.saleType = com.example.promotionservice.entity.SaleType.FLASH then 0 else 1 end asc, deal.startAt asc")
    List<FlashDealItem> findActive(@Param("productId") String productId, @Param("variantId") String variantId, @Param("now") Instant now);

    @Query("select item from FlashDealItem item join fetch item.flashDeal deal where item.productId = :productId and (item.variantId = :variantId or (item.variantId is null and :variantId is null)) and deal.id <> :excludedDealId and deal.startAt < :endAt and deal.endAt > :startAt")
    List<FlashDealItem> findOverlappingCampaignItems(@Param("productId") String productId, @Param("variantId") String variantId,
                                                      @Param("startAt") Instant startAt, @Param("endAt") Instant endAt,
                                                      @Param("excludedDealId") UUID excludedDealId);

    @Modifying
    @Query("update FlashDealItem item set item.initialQuota = item.quota where item.initialQuota is null")
    int backfillInitialQuota();

    @Modifying
    @Query(value = "update flash_deal_items item set quota = quota - :quantity where item.id = :itemId and item.quota >= :quantity and exists (select 1 from flash_deals deal where deal.id = item.flash_deal_id and deal.status = 'LIVE' and deal.start_at <= :now and deal.end_at > :now)", nativeQuery = true)
    int reserve(@Param("itemId") UUID itemId, @Param("quantity") int quantity, @Param("now") Instant now);

    @Modifying
    @Query("update FlashDealItem item set item.quota = item.quota + :quantity where item.id = :itemId")
    int release(@Param("itemId") UUID itemId, @Param("quantity") int quantity);
}
