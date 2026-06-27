package com.example.productservice.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CategoryDetailResponse(
        String id,
        String name,
        String slug,
        String description,
        Instant createdAt

) {

}
