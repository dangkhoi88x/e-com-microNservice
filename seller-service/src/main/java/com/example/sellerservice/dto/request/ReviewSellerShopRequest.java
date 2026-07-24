package com.example.sellerservice.dto.request;

import com.example.sellerservice.entity.SellerReviewAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewSellerShopRequest(
        @NotNull SellerReviewAction action,
        @Size(max = 1_000) String note
) {
}
