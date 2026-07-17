package com.example.productservice.dto.request;
import java.util.List;
public record ProductOptionRequest(String name, String displayName, String displayType, Integer displayOrder, Boolean required, List<ProductOptionValueRequest> values) {}
