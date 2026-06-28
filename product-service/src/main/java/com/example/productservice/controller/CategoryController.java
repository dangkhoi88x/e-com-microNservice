package com.example.productservice.controller;

import com.example.productservice.dto.request.CreateCategoryRequest;
import com.example.productservice.dto.request.UpdateCategoryRequest;
import com.example.productservice.dto.response.ApiResponse;
import com.example.productservice.dto.response.CategoryDetailResponse;
import com.example.productservice.dto.response.CreateCategoryResponse;
import com.example.productservice.dto.response.UpdateCategoryResponse;
import com.example.productservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;
    @PostMapping
    ApiResponse<CreateCategoryResponse> createCategory(@RequestBody @Valid CreateCategoryRequest request) {
        var data = categoryService.createCategory(request);
        return ApiResponse.<CreateCategoryResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Category created successfully")
                .data(data)
                .build();

    }
    @GetMapping
  ApiResponse<List<CategoryDetailResponse>> getCategories() {
        var data = categoryService.getAllCategories();
return ApiResponse.<List<CategoryDetailResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Categories retrieved successfully")
                .data(data)
                .build();

    }

    @GetMapping("/{id}")
    ApiResponse<CategoryDetailResponse> getCategoryDetail(@PathVariable String id) {
        var data = categoryService.getCategoryDetail(id);
        return ApiResponse.<CategoryDetailResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(data)
                .build();

    }

    @GetMapping("/slug/{slug}")
   ApiResponse<CategoryDetailResponse> getCategoryDetailBySlug(@PathVariable String slug) {
        var data = categoryService.getCategoryDetailBySlug(slug);
       return ApiResponse.<CategoryDetailResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(data)
                .build();

    }

    @PutMapping("/{id}")
   ApiResponse<UpdateCategoryResponse> updateCategory(@PathVariable String id, @RequestBody @Valid UpdateCategoryRequest request) {
        var data = categoryService.updateCategory(id, request);
       return ApiResponse.<UpdateCategoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Category updated successfully")
                .data(data)
                .build();

    }
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
