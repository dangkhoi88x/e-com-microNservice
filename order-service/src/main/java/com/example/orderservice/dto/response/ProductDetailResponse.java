package com.example.orderservice.dto.response;

import java.math.BigDecimal;

public record ProductDetailResponse(String id,
                                    String name,
                                    String description,
                                    BigDecimal price,
                                    Integer quantity,
                                    String status) {
}
