package com.example.productservice.dto.response;
import lombok.Builder;
import java.util.List;
@Builder
public record ProductOptionResponse(String id, String name, String displayName, String displayType, Integer displayOrder, Boolean required, List<ProductOptionValueResponse> values) {}
