package com.example.searchservice.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PriceStats(
        BigDecimal min,
        BigDecimal max,
        BigDecimal avg,
        long count
) {
}
