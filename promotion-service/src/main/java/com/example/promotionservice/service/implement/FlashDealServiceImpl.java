package com.example.promotionservice.service.implement;

import com.example.promotionservice.dto.request.*;
import com.example.promotionservice.dto.response.*;
import com.example.promotionservice.entity.*;
import com.example.promotionservice.repository.FlashDealRepository;
import com.example.promotionservice.repository.FlashDealItemRepository;
import com.example.promotionservice.repository.FlashDealReservationRepository;
import com.example.promotionservice.repository.FlashDealNotificationSubscriptionRepository;
import com.example.promotionservice.repository.FlashDealItemMetricProjection;
import com.example.promotionservice.repository.FlashDealCampaignMetricProjection;
import com.example.promotionservice.exception.PromotionServiceException;
import com.example.promotionservice.exception.ErrorCode;
import com.example.promotionservice.service.FlashDealService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class FlashDealServiceImpl implements FlashDealService {
    private final FlashDealRepository repository;
    private final FlashDealItemRepository itemRepository;
    private final FlashDealReservationRepository reservationRepository;
    private final FlashDealNotificationSubscriptionRepository notificationSubscriptionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Override @Transactional public FlashDealResponse create(CreateFlashDealRequest request) { return save(new FlashDeal(), request); }
    @Override @Transactional public List<FlashDealResponse> getAll(String status) {
        synchronizeStatuses();
        List<FlashDeal> deals = status == null || status.isBlank() ? repository.findAllByOrderByStartAtDesc() : repository.findAllByStatusOrderByStartAtAsc(FlashDealStatus.valueOf(status.toUpperCase()));
        return deals.stream().map(this::response).toList();
    }
    @Override @Transactional public List<FlashDealResponse> getByStatusAndType(FlashDealStatus status, SaleType saleType) {
        synchronizeStatuses();
        return repository.findAllByStatusOrderByStartAtAsc(status).stream()
                .filter(deal -> saleType == null || deal.effectiveSaleType() == saleType)
                .map(this::response)
                .toList();
    }
    @Override @Transactional public FlashDealResponse getById(String id) { synchronizeStatuses(); return response(find(id)); }
    @Override @Transactional public FlashDealDetailResponse getDetail(String id) {
        synchronizeStatuses();
        itemRepository.backfillInitialQuota();
        FlashDeal deal = find(id);
        Map<UUID, FlashDealItemMetricProjection> itemMetrics = reservationRepository.summarizeConfirmedByItem(deal.getId(), FlashDealReservationStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(FlashDealItemMetricProjection::getItemId, metric -> metric));
        FlashDealCampaignMetricProjection campaignMetric = reservationRepository.summarizeCampaign(deal.getId(), FlashDealReservationStatus.CONFIRMED);
        Instant now = Instant.now();
        Instant countdownTarget = deal.getStatus() == FlashDealStatus.SCHEDULED ? deal.getStartAt() : deal.getEndAt();
        long remainingSeconds = Math.max(0, countdownTarget.getEpochSecond() - now.getEpochSecond());
        List<FlashDealItemDetailResponse> items = deal.getItems().stream().map(item -> {
            FlashDealItemMetricProjection metric = itemMetrics.get(item.getId());
            Integer initialQuota = item.isQuotaLimited() ? item.effectiveInitialQuota() : null;
            Integer remainingQuota = item.isQuotaLimited() ? item.getQuota() : null;
            Integer usedQuota = item.isQuotaLimited() ? Math.max(0, initialQuota - remainingQuota) : null;
            return new FlashDealItemDetailResponse(item.getId(), item.getProductId(), item.getVariantId(), item.getOriginalPrice(), item.getSalePrice(), item.getDiscountPercent(), item.isQuotaLimited(), initialQuota, usedQuota, remainingQuota, metric == null ? 0 : metric.getOrderCount(), metric == null || metric.getRevenue() == null ? BigDecimal.ZERO : metric.getRevenue());
        }).toList();
        return new FlashDealDetailResponse(deal.getId(), deal.getName(), deal.getDescription(), deal.getStatus(), deal.effectiveSaleType(), deal.getStartAt(), deal.getEndAt(), countdownTarget, remainingSeconds, campaignMetric == null ? 0 : campaignMetric.getOrderCount(), campaignMetric == null || campaignMetric.getSoldQuantity() == null ? 0 : campaignMetric.getSoldQuantity(), campaignMetric == null || campaignMetric.getRevenue() == null ? BigDecimal.ZERO : campaignMetric.getRevenue(), items);
    }
    @Override @Transactional public FlashDealResponse update(String id, CreateFlashDealRequest request) { return save(find(id), request); }
    @Override @Transactional public void delete(String id) { repository.delete(find(id)); }
    @Override @Transactional public List<FlashDealPriceResponse> reserve(ReserveFlashDealRequest request) {
        synchronizeStatuses();
        List<FlashDealPriceResponse> result = new ArrayList<>();
        Instant now = Instant.now();
        for (FlashDealOrderItemRequest requested : request.items()) {
            List<FlashDealItem> candidates = itemRepository.findActive(requested.productId(), requested.variantId(), now);
            if (candidates.isEmpty()) continue;
            boolean applied = false;
            for (FlashDealItem item : candidates) {
                if (item.isQuotaLimited() && itemRepository.reserve(item.getId(), requested.quantity(), now) != 1) {
                    continue;
                }
                reservationRepository.save(FlashDealReservation.builder().orderId(request.orderId()).flashDealItem(item).productId(item.getProductId()).variantId(item.getVariantId()).quantity(requested.quantity()).salePrice(item.getSalePrice()).status(FlashDealReservationStatus.RESERVED).build());
                if (item.isQuotaLimited()) repository.markSoldOut(item.getFlashDeal().getId());
                result.add(new FlashDealPriceResponse(item.getId(), item.getProductId(), item.getVariantId(), item.getOriginalPrice(), item.getSalePrice(), requested.quantity()));
                applied = true;
                break;
            }
            if (!applied) throw new PromotionServiceException(ErrorCode.FLASH_SALE_SOLD_OUT);
        }
        return result;
    }
    @Override @Transactional public void confirm(String orderId) { reservationRepository.findAllByOrderId(orderId).stream().filter(r -> r.getStatus() == FlashDealReservationStatus.RESERVED).forEach(r -> r.setStatus(FlashDealReservationStatus.CONFIRMED)); }
    @Override @Transactional public void release(String orderId) { reservationRepository.findAllByOrderId(orderId).stream().filter(r -> r.getStatus() == FlashDealReservationStatus.RESERVED).forEach(r -> { if (r.getFlashDealItem().isQuotaLimited()) { itemRepository.release(r.getFlashDealItem().getId(), r.getQuantity()); if (r.getFlashDealItem().getFlashDeal().getStatus() == FlashDealStatus.SOLD_OUT) r.getFlashDealItem().getFlashDeal().setStatus(FlashDealStatus.LIVE); } r.setStatus(FlashDealReservationStatus.RELEASED); }); }
    @Scheduled(fixedDelayString = "${flash-deal.status-refresh-ms:60000}")
    @Transactional
    public void refreshStatuses() { synchronizeStatuses(); notifyUpcomingSubscribers(); }
    private void synchronizeStatuses() { Instant now = Instant.now(); repository.markEnded(now); repository.markLive(now); }
    @Override @Transactional public void subscribeForNotification(String flashDealId, String userId) {
        FlashDeal deal = find(flashDealId);
        if (deal.effectiveSaleType() != SaleType.FLASH || deal.getStatus() != FlashDealStatus.SCHEDULED || !deal.getStartAt().isAfter(Instant.now())) throw new PromotionServiceException(ErrorCode.FLASH_SALE_NOTIFICATION_UNAVAILABLE);
        if (!notificationSubscriptionRepository.existsByUserIdAndFlashDealId(userId, deal.getId())) notificationSubscriptionRepository.save(FlashDealNotificationSubscription.builder().userId(userId).flashDeal(deal).build());
    }
    @Override @Transactional(readOnly = true) public List<String> getNotificationSubscriptions(String userId) { return notificationSubscriptionRepository.findAllByUserId(userId).stream().map(subscription -> subscription.getFlashDeal().getId().toString()).toList(); }
    private void notifyUpcomingSubscribers() {
        Instant now = Instant.now();
        notificationSubscriptionRepository.findAllByNotifiedAtIsNullAndFlashDealStatusAndFlashDealStartAtAfterAndFlashDealStartAtLessThanEqual(FlashDealStatus.SCHEDULED, now, now.plusSeconds(15 * 60)).forEach(subscription -> {
            FlashDeal deal = subscription.getFlashDeal();
            kafkaTemplate.send("flash-sale-upcoming", subscription.getUserId(), Map.of("userId", subscription.getUserId(), "flashDealId", deal.getId().toString(), "flashDealName", deal.getName(), "startAt", deal.getStartAt().toString()));
            subscription.setNotifiedAt(now);
        });
    }
    private FlashDealResponse save(FlashDeal deal, CreateFlashDealRequest request) {
        if (!request.endAt().isAfter(request.startAt())) throw new PromotionServiceException(ErrorCode.INVALID_PROMOTION_PERIOD);
        SaleType saleType = request.saleType() == null ? SaleType.FLASH : request.saleType();
        validateNoOverlappingCampaign(deal, request, saleType);
        deal.setName(request.name().trim()); deal.setDescription(request.description()); deal.setStartAt(request.startAt()); deal.setEndAt(request.endAt());
        deal.setSaleType(saleType);
        deal.setStatus(request.status() == null ? FlashDealStatus.DRAFT : request.status());
        List<FlashDealItem> items = request.items().stream().map(i -> {
            boolean quotaLimited = saleType == SaleType.FLASH || Boolean.TRUE.equals(i.quotaLimited());
            if (quotaLimited && (i.quota() == null || i.quota() <= 0)) throw new PromotionServiceException(ErrorCode.INVALID_REQUEST);
            return FlashDealItem.builder().productId(i.productId()).variantId(i.variantId()).originalPrice(i.originalPrice()).discountPercent(i.discountPercent()).salePrice(i.originalPrice().multiply(BigDecimal.ONE.subtract(i.discountPercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))).setScale(2, RoundingMode.HALF_UP)).quota(quotaLimited ? i.quota() : 0).initialQuota(quotaLimited ? i.quota() : null).quotaLimited(quotaLimited).build();
        }).collect(Collectors.toList());
        deal.replaceItems(items); return response(repository.save(deal));
    }
    private void validateNoOverlappingCampaign(FlashDeal deal, CreateFlashDealRequest request, SaleType saleType) {
        Set<String> requestedProducts = new HashSet<>();
        UUID excludedDealId = deal.getId() == null ? UUID.randomUUID() : deal.getId();
        for (FlashDealItemRequest item : request.items()) {
            String key = item.productId() + ":" + (item.variantId() == null ? "" : item.variantId());
            if (!requestedProducts.add(key)) throw new PromotionServiceException(ErrorCode.SALE_CAMPAIGN_OVERLAP);
            boolean hasSameTypeOverlap = itemRepository.findOverlappingCampaignItems(item.productId(), item.variantId(), request.startAt(), request.endAt(), excludedDealId).stream()
                    .map(FlashDealItem::getFlashDeal)
                    .anyMatch(existing -> isBlocking(existing) && existing.effectiveSaleType() == saleType);
            if (hasSameTypeOverlap) throw new PromotionServiceException(ErrorCode.SALE_CAMPAIGN_OVERLAP);
        }
    }
    private boolean isBlocking(FlashDeal deal) { return deal.getStatus() == FlashDealStatus.DRAFT || deal.getStatus() == FlashDealStatus.SCHEDULED || deal.getStatus() == FlashDealStatus.LIVE; }
    private FlashDealResponse response(FlashDeal d) { return new FlashDealResponse(d.getId(), d.getName(), d.getDescription(), d.getStatus(), d.effectiveSaleType(), d.getStartAt(), d.getEndAt(), d.getItems().stream().map(i -> new FlashDealItemResponse(i.getId(), i.getProductId(), i.getVariantId(), i.getOriginalPrice(), i.getSalePrice(), i.getDiscountPercent(), i.getQuota(), i.effectiveInitialQuota(), i.isQuotaLimited())).toList()); }
    private FlashDeal find(String id) { return repository.findById(UUID.fromString(id)).orElseThrow(() -> new IllegalArgumentException("Flash deal not found")); }
}
