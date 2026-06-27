package com.example.productservice.service;

import com.example.productservice.dto.request.CreateCategoryRequest;
import com.example.productservice.dto.request.UpdateCategoryRequest;
import com.example.productservice.dto.response.CategoryDetailResponse;
import com.example.productservice.dto.response.CreateCategoryResponse;
import com.example.productservice.dto.response.UpdateCategoryResponse;

import java.util.List;

public interface CategoryService {
    CreateCategoryResponse createCategory(CreateCategoryRequest request);
    List<CategoryDetailResponse> getAllCategories();
    CategoryDetailResponse getCategoryDetail(String id);
    CategoryDetailResponse getCategoryDetailBySlug(String slug);
    UpdateCategoryResponse updateCategory(String id, UpdateCategoryRequest request);
    void deleteCategory(String id);
}
