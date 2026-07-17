package com.example.cartservice.client;

import com.example.cartservice.dto.request.AddCartItemRequest;
import com.example.cartservice.dto.response.ApiResponse;
import com.example.cartservice.dto.response.ProductSnapshot;
import com.example.cartservice.exception.CartServiceException;
import com.example.cartservice.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${services.product.base-url}")
    private String productServiceBaseUrl;

    public ProductSnapshot getSnapshot(AddCartItemRequest request) {
        try {
            ApiResponse<ProductResponse> response = restClientBuilder
                    .baseUrl(productServiceBaseUrl)
                    .build()
                    .get()
                    .uri("/api/v1/products/{id}", request.productId())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.getData() == null) {
                throw new CartServiceException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            ProductResponse product = response.getData();
            if (!"ACTIVE".equals(product.status())) {
                throw new CartServiceException(ErrorCode.PRODUCT_NOT_ACTIVE);
            }

            if (request.variantId() == null || request.variantId().isBlank()) {
                return new ProductSnapshot(
                        product.id(), null, product.name(), null,
                        product.price(), primaryImageUrl(product.images())
                );
            }

            ProductVariantResponse variant = product.variants().stream()
                    .filter(item -> request.variantId().equals(item.id()))
                    .findFirst()
                    .orElseThrow(() -> new CartServiceException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

            if (!"ACTIVE".equals(variant.status())) {
                throw new CartServiceException(ErrorCode.PRODUCT_NOT_ACTIVE);
            }

            return new ProductSnapshot(
                    product.id(), variant.id(), product.name(), formatAttributes(variant.attributes()),
                    variant.price(), variant.imageUrl() != null ? variant.imageUrl() : primaryImageUrl(product.images())
            );
        } catch (CartServiceException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CartServiceException(ErrorCode.PRODUCT_SERVICE_UNAVAILABLE);
        }
    }

    private String primaryImageUrl(List<ProductImageResponse> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(image -> Boolean.TRUE.equals(image.isPrimary()))
                .findFirst()
                .orElse(images.getFirst())
                .url();
    }

    private String formatAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        return attributes.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((left, right) -> left + " · " + right)
                .orElse(null);
    }

    private record ProductResponse(
            String id, String name, BigDecimal price, List<ProductImageResponse> images,
            List<ProductVariantResponse> variants, String status
    ) {
    }

    private record ProductVariantResponse(
            String id, Map<String, String> attributes, BigDecimal price, String imageUrl, String status
    ) {
    }

    private record ProductImageResponse(String url, Boolean isPrimary) {
    }
}
