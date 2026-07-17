package com.example.productservice.dto.response;
import lombok.Builder;
@Builder
public record ProductOptionValueResponse(String id, String value, String displayValue, String colorHex, String imageUrl, Integer displayOrder, Boolean active) {}
