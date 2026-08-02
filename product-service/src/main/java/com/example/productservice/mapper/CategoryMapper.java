package com.example.productservice.mapper;

import com.example.productservice.dto.response.CategoryDetailResponse;
import com.example.productservice.dto.response.CreateCategoryResponse;
import com.example.productservice.dto.response.UpdateCategoryResponse;
import com.example.productservice.entity.Category;
import org.springframework.stereotype.Component;

/**
 * Converts Category entities into API response DTOs.
 */
@Component
public class CategoryMapper {

    public CreateCategoryResponse toCreateResponse(Category category) {
        return CreateCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }

    public UpdateCategoryResponse toUpdateResponse(Category category) {
        return UpdateCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }

    public CategoryDetailResponse toDetailResponse(Category category) {
        return CategoryDetailResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
