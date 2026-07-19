package com.example.wishlistservice.client;

import com.example.wishlistservice.dto.response.ApiResponse;
import com.example.wishlistservice.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.math.BigDecimal;
import java.util.List;

@Component @RequiredArgsConstructor
public class ProductClient {
    private final RestClient.Builder restClientBuilder;
    @Value("${services.product.base-url}") private String productServiceBaseUrl;
    public ProductSnapshot getSnapshot(String productId, String variantId) {
        try {
            ApiResponse<ProductResponse> response = restClientBuilder.baseUrl(productServiceBaseUrl).build().get().uri("/api/v1/products/{id}", productId).retrieve().body(new ParameterizedTypeReference<>() {});
            if (response == null || response.getData() == null) throw new WishlistServiceException(ErrorCode.PRODUCT_NOT_FOUND);
            ProductResponse product = response.getData();
            if (!"ACTIVE".equals(product.status())) throw new WishlistServiceException(ErrorCode.PRODUCT_NOT_ACTIVE);
            if (variantId == null || variantId.isBlank()) return new ProductSnapshot(product.id(), null, product.name(), product.price(), primaryImage(product.images()), product.categoryName());
            ProductVariantResponse variant = product.variants() == null ? null : product.variants().stream().filter(item -> variantId.equals(item.id())).findFirst().orElse(null);
            if (variant == null) throw new WishlistServiceException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
            if (!"ACTIVE".equals(variant.status())) throw new WishlistServiceException(ErrorCode.PRODUCT_NOT_ACTIVE);
            return new ProductSnapshot(product.id(), variant.id(), product.name(), variant.price(), variant.imageUrl() == null ? primaryImage(product.images()) : variant.imageUrl(), product.categoryName());
        } catch (WishlistServiceException exception) { throw exception; }
        catch (RestClientException exception) { throw new WishlistServiceException(ErrorCode.PRODUCT_SERVICE_UNAVAILABLE); }
    }
    private String primaryImage(List<ProductImageResponse> images) { if (images == null || images.isEmpty()) return null; return images.stream().filter(image -> Boolean.TRUE.equals(image.isPrimary())).findFirst().orElse(images.getFirst()).url(); }
    private record ProductResponse(String id, String name, String categoryName, BigDecimal price, List<ProductImageResponse> images, List<ProductVariantResponse> variants, String status) { }
    private record ProductVariantResponse(String id, BigDecimal price, String imageUrl, String status) { }
    private record ProductImageResponse(String url, Boolean isPrimary) { }
}
