package com.example.promotionservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PromotionJsonMapper {
    private final ObjectMapper objectMapper;

    public String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Collections.emptyList() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid promotion scope", exception);
        }
    }

    public List<String> fromJson(String value) {
        try {
            return value == null || value.isBlank()
                    ? Collections.emptyList()
                    : objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }
}
