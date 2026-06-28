package com.example.productservice.controller;

import com.example.productservice.dto.request.CreateProductRequest;
import com.example.productservice.dto.response.ApiResponse;
import com.example.productservice.dto.response.CreateProductResponse;
import com.example.productservice.dto.response.ProductDetailResponse;
import com.example.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    // annotation của Spring Security để lấy thông tin user từ JWT token đã được decode
    //@AuthenticationPrincipal: Lấy thông tin user đang authenticated
    //Jwt jwt: Object chứa thông tin JWT đã decode (payload + header)
    @PostMapping
    ApiResponse<CreateProductResponse> createProduct(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateProductRequest request
    ) {
        var data = productService.createProduct(jwt.getSubject(), request);
        return ApiResponse.<CreateProductResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Product created successfully")
                .data(data)
                .build();
    }

    @GetMapping
    ApiResponse<List<ProductDetailResponse>> getAllProducts() {
        var data = productService.getAllProducts();
        return ApiResponse.<List<ProductDetailResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Products retrieved successfully")
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

    @DeleteMapping("/{id}")
    ApiResponse<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Product deleted successfully")
                .build();
    }
}
