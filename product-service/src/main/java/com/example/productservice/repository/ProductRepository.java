package com.example.productservice.repository;

import com.example.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, String id);

    boolean existsByCategoryId(String categoryId);

    Optional<Product> findBySlug(String slug);
}
