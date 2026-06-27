package com.example.productservice.repository;

import com.example.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    boolean existsBySlug(String slug);

    boolean existsByCategoryId(String categoryId);

    Optional<Product> findBySlug(String slug);
}
