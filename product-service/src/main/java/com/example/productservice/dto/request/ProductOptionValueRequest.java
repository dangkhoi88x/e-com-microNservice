package com.example.productservice.dto.request;
public record ProductOptionValueRequest(String value, String displayValue, String colorHex, String imageUrl, Integer displayOrder, Boolean active) {}
