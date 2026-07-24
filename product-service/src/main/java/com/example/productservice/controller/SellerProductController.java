package com.example.productservice.controller;

import com.example.productservice.dto.request.CreateSellerProductRequest;
import com.example.productservice.dto.request.UpdateSellerProductRequest;
import com.example.productservice.dto.response.ApiResponse;
import com.example.productservice.dto.response.CreateProductResponse;
import com.example.productservice.dto.response.PageResponse;
import com.example.productservice.dto.response.ProductDetailResponse;
import com.example.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seller/products")
@PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'SELLER')")
public class SellerProductController {
    private final ProductService productService;

    @PostMapping
    public ApiResponse<CreateProductResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody CreateSellerProductRequest request
    ) {
        return response(HttpStatus.CREATED, "Product draft created successfully",
                productService.createSellerProduct(jwt.getSubject(), authorization, request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductDetailResponse>> getMine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return response(HttpStatus.OK, "Seller products retrieved successfully",
                productService.getMySellerProducts(jwt.getSubject(), page, size));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDetailResponse> update(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateSellerProductRequest request
    ) {
        return response(HttpStatus.OK, "Product draft updated successfully",
                productService.updateSellerProduct(id, jwt.getSubject(), request));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<ProductDetailResponse> submit(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return response(HttpStatus.OK, "Product submitted for approval",
                productService.submitSellerProduct(id, jwt.getSubject(), authorization));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        productService.deleteProduct(id);
        return response(HttpStatus.OK, "Product deleted successfully", null);
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).data(data).build();
    }
}
