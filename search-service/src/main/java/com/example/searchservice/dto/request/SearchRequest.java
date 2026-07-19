package com.example.searchservice.dto.request;

public record SearchRequest(String q,
                            String categoryId,
                            String name,
                            String description,
                            Double minPrice,
                            Double maxPrice,
                            String status,
                            Boolean inStock) {
}
