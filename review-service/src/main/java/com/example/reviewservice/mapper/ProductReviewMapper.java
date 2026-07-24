package com.example.reviewservice.mapper;

import com.example.reviewservice.dto.request.CreateReviewRequest;
import com.example.reviewservice.dto.request.UpdateReviewRequest;
import com.example.reviewservice.dto.response.ProductReviewResponse;
import com.example.reviewservice.dto.response.ReviewEligibilityResponse;
import com.example.reviewservice.entity.ProductReview;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProductReviewMapper {
    @Mapping(target = "reviewerName", expression = "java(publicReviewerName(review))")
    ProductReviewResponse toResponse(ProductReview review);

    default String publicReviewerName(ProductReview review) {
        return review.getReviewerName() == null || review.getReviewerName().isBlank()
                ? "Khách hàng NovaShop"
                : review.getReviewerName();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "sellerReply", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "sellerId", source = "eligibility.sellerId")
    @Mapping(target = "reviewerName", source = "reviewerName")
    @Mapping(target = "verifiedPurchase", constant = "true")
    @Mapping(target = "orderId", source = "eligibility.orderId")
    @Mapping(target = "orderItemId", source = "eligibility.orderItemId")
    @Mapping(target = "productId", source = "eligibility.productId")
    @Mapping(target = "variantId", source = "eligibility.variantId")
    @Mapping(target = "rating", source = "request.rating")
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "images", source = "request.images")
    ProductReview toEntity(
            CreateReviewRequest request,
            ReviewEligibilityResponse eligibility,
            String userId,
            String reviewerName
    );

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "variantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "sellerId", ignore = true)
    @Mapping(target = "reviewerName", ignore = true)
    @Mapping(target = "verifiedPurchase", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "orderItemId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sellerReply", ignore = true)
    void update(UpdateReviewRequest request, @MappingTarget ProductReview review);
}
