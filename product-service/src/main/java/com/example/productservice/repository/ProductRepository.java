package com.example.productservice.repository;

import com.example.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;
import com.example.productservice.common.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, String id);

    boolean existsByCategoryId(String categoryId);

    Optional<Product> findBySlug(String slug);

    Page<Product> findAllBySellerIdOrderByCreatedAtDesc(String sellerId, Pageable pageable);

    Optional<Product> findByIdAndSellerId(String id, String sellerId);

    List<Product> findAllByShopIdAndStatus(String shopId, ProductStatus status);
}
