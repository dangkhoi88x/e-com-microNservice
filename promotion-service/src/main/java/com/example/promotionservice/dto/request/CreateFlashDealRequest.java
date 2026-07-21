package com.example.promotionservice.dto.request;

import com.example.promotionservice.entity.FlashDealStatus;
import com.example.promotionservice.entity.SaleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public record CreateFlashDealRequest(@NotBlank String name, String description,
                                     @NotNull Instant startAt, @NotNull Instant endAt,
                                     FlashDealStatus status, SaleType saleType,
                                     @NotEmpty List<@Valid FlashDealItemRequest> items) {}
