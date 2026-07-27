package com.example.productservice.dto.request;

import com.example.productservice.common.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSellerProductStatusRequest(
        @NotNull ProductStatus status
) {
}
