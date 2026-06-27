package com.example.productservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest ( @NotBlank(message = "Name is required")
                                      String name,
                                      String description) {

}
