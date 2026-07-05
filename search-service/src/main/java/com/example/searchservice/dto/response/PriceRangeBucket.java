package com.example.searchservice.dto.response;

import lombok.Builder;

@Builder
public record PriceRangeBucket(
        String range,
        long count
) {
}
