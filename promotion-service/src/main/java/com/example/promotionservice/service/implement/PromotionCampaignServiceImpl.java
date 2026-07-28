package com.example.promotionservice.service.implement;

import com.example.promotionservice.dto.request.CreatePromotionCampaignRequest;
import com.example.promotionservice.dto.request.UpdatePromotionCampaignRequest;
import com.example.promotionservice.dto.response.PromotionCampaignResponse;
import com.example.promotionservice.configuration.PromotionRedisCacheConfiguration;
import com.example.promotionservice.entity.PromotionCampaign;
import com.example.promotionservice.entity.PromotionStatus;
import com.example.promotionservice.exception.ErrorCode;
import com.example.promotionservice.exception.PromotionServiceException;
import com.example.promotionservice.mapper.PromotionCampaignMapper;
import com.example.promotionservice.repository.PromotionCampaignRepository;
import com.example.promotionservice.service.PromotionCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionCampaignServiceImpl implements PromotionCampaignService {
    private final PromotionCampaignRepository repository;
    private final PromotionCampaignMapper mapper;

    @Override
    @CacheEvict(cacheNames = PromotionRedisCacheConfiguration.ACTIVE_PROMOTIONS_CACHE, allEntries = true)
    public PromotionCampaignResponse create(CreatePromotionCampaignRequest request) {
        validateDates(request.startAt(), request.endAt());
        String code = normalizeCode(request.code());
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new PromotionServiceException(ErrorCode.PROMOTION_CODE_EXISTS);
        }

        PromotionCampaign campaign = mapper.toEntity(request);
        campaign.setName(request.name().trim());
        campaign.setCode(code);
        campaign.setUsedCount(0);
        campaign.setStatus(PromotionStatus.DRAFT);
        return mapper.toResponse(repository.save(campaign));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = PromotionRedisCacheConfiguration.ACTIVE_PROMOTIONS_CACHE,
            key = "'all'",
            condition = "#status != null && #status.equalsIgnoreCase('ACTIVE')"
    )
    public List<PromotionCampaignResponse> getAll(String status) {
        List<PromotionCampaign> campaigns = status == null || status.isBlank()
                ? repository.findAll()
                : findByStatus(status);
        return campaigns.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionCampaignResponse getById(String id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @CacheEvict(cacheNames = PromotionRedisCacheConfiguration.ACTIVE_PROMOTIONS_CACHE, allEntries = true)
    public PromotionCampaignResponse update(String id, UpdatePromotionCampaignRequest request) {
        validateDates(request.startAt(), request.endAt());
        PromotionCampaign campaign = find(id);
        mapper.updateEntity(request, campaign);
        campaign.setName(request.name().trim());
        return mapper.toResponse(repository.save(campaign));
    }

    @Override
    @CacheEvict(cacheNames = PromotionRedisCacheConfiguration.ACTIVE_PROMOTIONS_CACHE, allEntries = true)
    public void delete(String id) {
        PromotionCampaign campaign = find(id);
        if (campaign.getUsedCount() > 0) {
            campaign.setStatus(PromotionStatus.INACTIVE);
            return;
        }
        repository.delete(campaign);
    }

    private PromotionCampaign find(String id) {
        try {
            return repository.findById(UUID.fromString(id))
                    .orElseThrow(() -> new PromotionServiceException(ErrorCode.PROMOTION_NOT_FOUND));
        } catch (IllegalArgumentException exception) {
            throw new PromotionServiceException(ErrorCode.PROMOTION_NOT_FOUND);
        }
    }

    private void validateDates(Instant startAt, Instant endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new PromotionServiceException(ErrorCode.INVALID_PROMOTION_PERIOD);
        }
    }

    private List<PromotionCampaign> findByStatus(String status) {
        try {
            return repository.findAllByStatusOrderByPriorityDescStartAtDesc(PromotionStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException exception) {
            throw new PromotionServiceException(ErrorCode.INVALID_REQUEST);
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }
}
