package com.example.promotionservice.dto.response;

import com.example.promotionservice.entity.FlashDealStatus;
import com.example.promotionservice.entity.SaleType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FlashDealResponse(UUID id, String name, String description, FlashDealStatus status,
                                SaleType saleType, Instant startAt, Instant endAt, List<FlashDealItemResponse> items) {}
