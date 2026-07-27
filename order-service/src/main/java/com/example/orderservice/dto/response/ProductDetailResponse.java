package com.example.orderservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(String id,
                                    String shopId,
                                    String sellerId,
                                    String name,
                                    String description,
                                    BigDecimal price,
                                    Integer quantity,
                                    List<ProductVariantResponse> variants,
                                    String status) {
}
