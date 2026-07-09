package com.example.productservice.service;

import com.example.productservice.dto.request.CreateProductRequest;
import com.example.productservice.dto.request.SearchRequest;
import com.example.productservice.dto.request.UpdateProductRequest;
import com.example.productservice.dto.response.CreateProductResponse;
import com.example.productservice.dto.response.PageResponse;
import com.example.productservice.dto.response.ProductDetailResponse;
import event.InventoryUpdatedEvent;

import java.util.List;

public interface ProductService {
    CreateProductResponse createProduct(String sellerId, CreateProductRequest request);
    PageResponse<ProductDetailResponse> getAllProducts(int page, int size, SearchRequest request);
    ProductDetailResponse getProductById(String id);
    ProductDetailResponse getProductBySlug(String slug);
    ProductDetailResponse updateProduct(String id, String userId, UpdateProductRequest request);
    void deleteProduct(String id);
    void syncStockFromInventoryEvent(InventoryUpdatedEvent event);

}
