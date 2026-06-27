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
    ResponseEntity<ApiResponse<CreateCategoryResponse>> createCategory(@RequestBody @Valid CreateCategoryRequest request) {
        var data = categoryService.createCategory(request);
        ApiResponse<CreateCategoryResponse> response = ApiResponse.<CreateCategoryResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Category created successfully")
                .data(data)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    ResponseEntity<ApiResponse<List<CategoryDetailResponse>>> getCategories() {
        var data = categoryService.getAllCategories();
        ApiResponse<List<CategoryDetailResponse>> response = ApiResponse.<List<CategoryDetailResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Categories retrieved successfully")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<CategoryDetailResponse>> getCategoryDetail(@PathVariable String id) {
        var data = categoryService.getCategoryDetail(id);
        ApiResponse<CategoryDetailResponse> response = ApiResponse.<CategoryDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    ResponseEntity<ApiResponse<CategoryDetailResponse>> getCategoryDetailBySlug(@PathVariable String slug) {
        var data = categoryService.getCategoryDetailBySlug(slug);
        ApiResponse<CategoryDetailResponse> response = ApiResponse.<CategoryDetailResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<UpdateCategoryResponse>> updateCategory(@PathVariable String id, @RequestBody @Valid UpdateCategoryRequest request) {
        var data = categoryService.updateCategory(id, request);
        ApiResponse<UpdateCategoryResponse> response = ApiResponse.<UpdateCategoryResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Category updated successfully")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
