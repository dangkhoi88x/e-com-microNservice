package com.example.promotionservice.mapper;

import com.example.promotionservice.dto.request.CreatePromotionCampaignRequest;
import com.example.promotionservice.dto.request.UpdatePromotionCampaignRequest;
import com.example.promotionservice.dto.response.PromotionCampaignResponse;
import com.example.promotionservice.entity.PromotionCampaign;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = PromotionJsonMapper.class)
public interface PromotionCampaignMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "usedCount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "applicableCategoryIdsJson", source = "applicableCategoryIds")
    @Mapping(target = "applicableProductIdsJson", source = "applicableProductIds")
    @Mapping(target = "priority", source = "priority", defaultValue = "0")
    @Mapping(target = "stackable", source = "stackable", defaultValue = "false")
    PromotionCampaign toEntity(CreatePromotionCampaignRequest request);

    @Mapping(target = "applicableCategoryIdsJson", source = "applicableCategoryIds")
    @Mapping(target = "applicableProductIdsJson", source = "applicableProductIds")
    @Mapping(target = "priority", source = "priority", defaultValue = "0")
    @Mapping(target = "stackable", source = "stackable", defaultValue = "false")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "usedCount", ignore = true)
    void updateEntity(UpdatePromotionCampaignRequest request, @MappingTarget PromotionCampaign campaign);

    @Mapping(target = "applicableCategoryIds", source = "applicableCategoryIdsJson")
    @Mapping(target = "applicableProductIds", source = "applicableProductIdsJson")
    PromotionCampaignResponse toResponse(PromotionCampaign campaign);
}
