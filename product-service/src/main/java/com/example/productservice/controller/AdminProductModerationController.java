package com.example.productservice.controller;

import com.example.productservice.common.ProductStatus;
import com.example.productservice.dto.request.ModerateProductRequest;
import com.example.productservice.dto.request.SearchRequest;
import com.example.productservice.dto.response.ApiResponse;
import com.example.productservice.dto.response.PageResponse;
import com.example.productservice.dto.response.ProductDetailResponse;
import com.example.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/products")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminProductModerationController {
    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResponse<ProductDetailResponse>> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            SearchRequest request
    ) {
        return response(HttpStatus.OK, "Products retrieved successfully",
                productService.getAllProducts(page, size, request));
    }

    @PutMapping("/{id}/review")
    public ApiResponse<ProductDetailResponse> review(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ModerateProductRequest request
    ) {
        return response(HttpStatus.OK, "Product moderation completed",
                productService.moderateProduct(id, jwt.getSubject(), request));
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).data(data).build();
    }
}
