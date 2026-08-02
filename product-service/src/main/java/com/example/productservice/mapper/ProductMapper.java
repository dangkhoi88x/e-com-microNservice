package com.example.productservice.mapper;

import com.example.productservice.dto.response.CreateProductResponse;
import com.example.productservice.dto.response.PageResponse;
import com.example.productservice.dto.response.ProductDetailResponse;
import com.example.productservice.dto.response.ProductOptionResponse;
import com.example.productservice.dto.response.ProductOptionValueResponse;
import com.example.productservice.dto.response.ProductVariantResponse;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductOption;
import com.example.productservice.entity.ProductVariant;
import event.ProductCreatedEvent;
import event.ProductUpdatedEvent;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converts Product entities into API responses and Kafka events.
 * This class contains mapping only; business validation stays in ProductServiceImpl.
 */
@Component
public class ProductMapper {

    public ProductCreatedEvent toCreatedEvent(Product product) {
        return ProductCreatedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice().doubleValue())
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .thumbnailUrl(getThumbnailUrl(product))
                .inStock(isInStock(product))
                .build();
    }

    public ProductUpdatedEvent toUpdatedEvent(Product product) {
        return ProductUpdatedEvent.builder()
                .productId(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice().doubleValue())
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .thumbnailUrl(getThumbnailUrl(product))
                .inStock(isInStock(product))
                .build();
    }

    public CreateProductResponse toCreateResponse(Product product) {
        return CreateProductResponse.builder()
                .id(product.getId())
                .shopId(product.getShopId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .images(product.getImages())
                .options(toOptionResponses(product.getOptions()))
                .variants(toVariantResponses(product.getVariants()))
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }

    public ProductDetailResponse toDetailResponse(Product product) {
        return toDetailResponse(product, product.getQuantity());
    }

    public ProductDetailResponse toDetailResponse(Product product, Integer quantity) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .shopId(product.getShopId())
                .sellerId(product.getSellerId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .categoryId(product.getCategory() == null ? null : product.getCategory().getId())
                .categoryName(product.getCategory() == null ? null : product.getCategory().getName())
                .price(product.getPrice())
                .quantity(quantity)
                .images(product.getImages())
                .options(toOptionResponses(product.getOptions()))
                .variants(toVariantResponses(product.getVariants()))
                .status(product.getStatus())
                .moderationNote(product.getModerationNote())
                .createdAt(product.getCreatedAt())
                .build();
    }

    public PageResponse<ProductDetailResponse> toPageResponse(
            Page<Product> productPage,
            List<ProductDetailResponse> content
    ) {
        return PageResponse.<ProductDetailResponse>builder()
                .currentPage(productPage.getNumber() + 1)
                .pageSize(productPage.getSize())
                .totalPages(productPage.getTotalPages())
                .totalElements(productPage.getTotalElements())
                .content(content)
                .build();
    }

    private List<ProductOptionResponse> toOptionResponses(List<ProductOption> options) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
                .map(option -> ProductOptionResponse.builder()
                        .id(option.getId())
                        .name(option.getName())
                        .displayName(option.getDisplayName())
                        .displayType(option.getDisplayType())
                        .displayOrder(option.getDisplayOrder())
                        .required(option.getRequired())
                        .values(option.getValues().stream()
                                .map(value -> ProductOptionValueResponse.builder()
                                        .id(value.getId())
                                        .value(value.getValue())
                                        .displayValue(value.getDisplayValue())
                                        .colorHex(value.getColorHex())
                                        .imageUrl(value.getImageUrl())
                                        .displayOrder(value.getDisplayOrder())
                                        .active(value.getActive())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private List<ProductVariantResponse> toVariantResponses(List<ProductVariant> variants) {
        if (variants == null) {
            return List.of();
        }

        return variants.stream()
                .map(variant -> ProductVariantResponse.builder()
                        .id(variant.getId())
                        .sku(variant.getSku())
                        .attributes(variant.getAttributes())
                        .price(variant.getPrice())
                        .quantity(variant.getQuantity())
                        .imageUrl(variant.getImageUrl())
                        .status(variant.getStatus())
                        .build())
                .toList();
    }

    private String getThumbnailUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }

        return product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .findFirst()
                .orElse(product.getImages().getFirst())
                .getUrl();
    }

    private boolean isInStock(Product product) {
        return product.getQuantity() != null && product.getQuantity() > 0;
    }
}
