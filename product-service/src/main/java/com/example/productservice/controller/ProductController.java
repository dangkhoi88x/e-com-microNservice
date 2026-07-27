package com.example.productservice.controller;

import com.example.productservice.dto.request.SearchRequest;
import com.example.productservice.dto.response.ApiResponse;
import com.example.productservice.dto.response.PageResponse;
import com.example.productservice.dto.response.ProductDetailResponse;
import com.example.productservice.common.ProductStatus;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    @GetMapping
    ApiResponse<PageResponse<ProductDetailResponse>> getProducts(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            SearchRequest request) {
        var data = productService.getAllProducts(page, size, new SearchRequest(
                request.categoryId(), request.name(), request.minPrice(), request.maxPrice(),
                ProductStatus.ACTIVE, request.inStock()
        ));
        return ApiResponse.<PageResponse<ProductDetailResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Products retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/slug/{slug}")
    ApiResponse<ProductDetailResponse> getProductBySlug(@PathVariable String slug) {
        var data = productService.getProductBySlug(slug);
        return ApiResponse.<ProductDetailResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Product retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    ApiResponse<ProductDetailResponse> getProductById(@PathVariable String id) {
        var data = productService.getProductById(id);
        return ApiResponse.<ProductDetailResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Product retrieved successfully")
                .data(data)
                .build();
    }

}
