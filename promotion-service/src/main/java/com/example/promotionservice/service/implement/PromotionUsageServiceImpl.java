package com.example.promotionservice.service.implement;

import com.example.promotionservice.dto.request.PromotionOrderRequest;
import com.example.promotionservice.dto.request.ReservePromotionRequest;
import com.example.promotionservice.dto.request.ValidatePromotionRequest;
import com.example.promotionservice.dto.response.PromotionCalculationResponse;
import com.example.promotionservice.entity.PromotionCampaign;
import com.example.promotionservice.entity.PromotionStatus;
import com.example.promotionservice.entity.PromotionUsage;
import com.example.promotionservice.entity.PromotionUsageStatus;
import com.example.promotionservice.exception.ErrorCode;
import com.example.promotionservice.exception.PromotionServiceException;
import com.example.promotionservice.repository.PromotionCampaignRepository;
import com.example.promotionservice.repository.PromotionUsageRepository;
import com.example.promotionservice.service.PromotionUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionUsageServiceImpl implements PromotionUsageService {
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionUsageRepository usageRepository;

    @Override
    public PromotionCalculationResponse validate(ValidatePromotionRequest request) {
        PromotionCampaign campaign = activeCampaign(request.campaignCode());
        BigDecimal discount = calculateDiscount(campaign, request.subtotalAmount());
        return calculation(campaign, request.subtotalAmount(), discount, "Promotion is applicable");
    }

    @Override
    public PromotionCalculationResponse reserve(ReservePromotionRequest request) {
        PromotionUsage existing = usageRepository.findByOrderId(request.orderId()).orElse(null);
        if (existing != null) {
            return calculation(existing.getCampaign(), request.subtotalAmount(), existing.getDiscountAmount(), "Promotion reservation already exists");
        }

        PromotionCampaign campaign = activeCampaign(request.campaignCode());
        BigDecimal discount = calculateDiscount(campaign, request.subtotalAmount());
        PromotionUsage usage = PromotionUsage.builder()
                .userId(request.userId())
                .orderId(request.orderId())
                .campaign(campaign)
                .discountAmount(discount)
                .status(PromotionUsageStatus.RESERVED)
                .expiresAt(Instant.now().plusSeconds(30 * 60L))
                .build();
        usageRepository.save(usage);
        return calculation(campaign, request.subtotalAmount(), discount, "Promotion reserved");
    }

    @Override
    public void confirm(PromotionOrderRequest request) {
        usageRepository.findByOrderId(request.orderId()).ifPresent(usage -> {
            if (usage.getStatus() != PromotionUsageStatus.RESERVED) return;
            usage.setStatus(PromotionUsageStatus.USED);
            PromotionCampaign campaign = usage.getCampaign();
            campaign.setUsedCount(campaign.getUsedCount() + 1);
            campaignRepository.save(campaign);
            usageRepository.save(usage);
        });
    }

    @Override
    public void release(PromotionOrderRequest request) {
        usageRepository.findByOrderId(request.orderId()).ifPresent(usage -> {
            if (usage.getStatus() == PromotionUsageStatus.RESERVED) {
                usage.setStatus(PromotionUsageStatus.RELEASED);
                usageRepository.save(usage);
            }
        });
    }

    private PromotionCampaign activeCampaign(String code) {
        PromotionCampaign campaign = campaignRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new PromotionServiceException(ErrorCode.PROMOTION_NOT_FOUND));
        Instant now = Instant.now();
        if (campaign.getStatus() != PromotionStatus.ACTIVE) {
            throw new PromotionServiceException(ErrorCode.PROMOTION_NOT_ACTIVE);
        }
        if (now.isBefore(campaign.getStartAt()) || !now.isBefore(campaign.getEndAt())) {
            throw new PromotionServiceException(ErrorCode.PROMOTION_EXPIRED);
        }
        if (campaign.getUsageLimit() > 0 && campaign.getUsedCount() >= campaign.getUsageLimit()) {
            throw new PromotionServiceException(ErrorCode.PROMOTION_USAGE_LIMIT_REACHED);
        }
        return campaign;
    }

    private BigDecimal calculateDiscount(PromotionCampaign campaign, BigDecimal subtotal) {
        if (subtotal.signum() < 0 || subtotal.compareTo(campaign.getMinOrderAmount()) < 0) {
            throw new PromotionServiceException(ErrorCode.PROMOTION_MIN_ORDER_NOT_MET);
        }
        BigDecimal discount = switch (campaign.getType()) {
            case PERCENTAGE -> subtotal.multiply(campaign.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> campaign.getDiscountValue();
            case FREE_SHIPPING -> BigDecimal.ZERO;
        };
        if (campaign.getMaxDiscountAmount() != null) discount = discount.min(campaign.getMaxDiscountAmount());
        return discount.max(BigDecimal.ZERO).min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private PromotionCalculationResponse calculation(PromotionCampaign campaign, BigDecimal subtotal, BigDecimal discount, String message) {
        return new PromotionCalculationResponse(true, campaign.getId().toString(), campaign.getCode(), discount, subtotal.subtract(discount), message);
    }
}
