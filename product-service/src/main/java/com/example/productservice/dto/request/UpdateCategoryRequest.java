package com.example.productservice.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(   @Size(min = 1, message = "Name must not be empty")
                                       String name,
                                       String description) {
}
