package com.example.productservice.service;

import com.example.productservice.dto.request.CreateProductRequest;
import com.example.productservice.dto.request.CreateSellerProductRequest;
import com.example.productservice.dto.request.ModerateProductRequest;
import com.example.productservice.dto.request.SearchRequest;
import com.example.productservice.dto.request.UpdateProductRequest;
import com.example.productservice.dto.request.UpdateSellerProductRequest;
import com.example.productservice.dto.request.UpdateSellerProductQuantityRequest;
import com.example.productservice.dto.request.UpdateSellerProductStatusRequest;
import com.example.productservice.dto.response.CreateProductResponse;
import com.example.productservice.dto.response.PageResponse;
import com.example.productservice.dto.response.ProductDetailResponse;
import event.InventoryUpdatedEvent;

import java.util.List;

public interface ProductService {
    CreateProductResponse createProduct(String sellerId, CreateProductRequest request);
    CreateProductResponse createSellerProduct(String sellerId, String authorization, CreateSellerProductRequest request);
    PageResponse<ProductDetailResponse> getAllProducts(int page, int size, SearchRequest request);
    PageResponse<ProductDetailResponse> getMySellerProducts(String sellerId, int page, int size);
    ProductDetailResponse getProductById(String id);
    ProductDetailResponse getSellerProductById(String id, String sellerId);
    ProductDetailResponse getProductBySlug(String slug);
    ProductDetailResponse updateProduct(String id, String userId, UpdateProductRequest request);
    ProductDetailResponse updateSellerProduct(String id, String sellerId, UpdateSellerProductRequest request);
    ProductDetailResponse updateSellerProductQuantity(String id, String sellerId, UpdateSellerProductQuantityRequest request);
    ProductDetailResponse updateSellerProductStatus(String id, String sellerId, UpdateSellerProductStatusRequest request);
    ProductDetailResponse submitSellerProduct(String id, String sellerId, String authorization);
    ProductDetailResponse moderateProduct(String id, String adminUserId, ModerateProductRequest request);
    void deleteProduct(String id);
    void syncStockFromInventoryEvent(InventoryUpdatedEvent event);
    void inactivateProductsForSuspendedShop(String shopId);

}
