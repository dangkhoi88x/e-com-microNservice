package com.example.productservice.service;

import com.example.productservice.dto.request.CreateProductRequest;
import com.example.productservice.dto.request.SearchRequest;
import com.example.productservice.dto.response.CreateProductResponse;
import com.example.productservice.dto.response.PageResponse;
import com.example.productservice.dto.response.ProductDetailResponse;

import java.util.List;

public interface ProductService {
    CreateProductResponse createProduct(String sellerId, CreateProductRequest request);
    PageResponse<ProductDetailResponse> getAllProducts(int page, int size, SearchRequest request);
    ProductDetailResponse getProductById(String id);
    void deleteProduct(String id);
}
