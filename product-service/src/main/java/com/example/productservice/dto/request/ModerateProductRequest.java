package com.example.productservice.dto.request;

import com.example.productservice.common.ProductModerationAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerateProductRequest(
        @NotNull ProductModerationAction action,
        @Size(max = 1_000) String note
) {
}
