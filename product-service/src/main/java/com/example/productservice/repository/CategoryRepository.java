package com.example.productservice.repository;

import com.example.productservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository  extends JpaRepository<Category, String> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);

}
