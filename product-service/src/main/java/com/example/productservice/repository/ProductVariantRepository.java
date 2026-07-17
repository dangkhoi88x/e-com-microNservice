package com.example.productservice.repository;

import com.example.productservice.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, String id);

    List<ProductVariant> findByProductId(String productId);

    Optional<ProductVariant> findByIdAndProductId(String id, String productId);
}
