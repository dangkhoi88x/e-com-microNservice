package com.example.productservice.repository.specification;

import com.example.productservice.common.ProductStatus;
import com.example.productservice.entity.Category;
import com.example.productservice.entity.Product;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
@RequiredArgsConstructor
public class ProductSpecification {

    public static Specification<Product> hasName(String name) {
        if(StringUtils.isBlank(name)) {
            return (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .like(root.get("name"), "%" + name + "%");
        }
    }

    public static Specification<Product> hasPrice(BigDecimal minPrice, BigDecimal maxPrice) {
        if(minPrice == null && maxPrice == null) {
            return (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        else if(minPrice != null && maxPrice == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .greaterThanOrEqualTo(root.get("price"), minPrice);
        } else if(minPrice == null) {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .lessThanOrEqualTo(root.get("price"), maxPrice);
        } else {
            return (root, query, criteriaBuilder) -> criteriaBuilder
                    .between(root.get("price"), minPrice, maxPrice);
        }
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        if(status == null) {
            return (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
        } else {
            return (root, _, criteriaBuilder) -> criteriaBuilder
                    .equal(root.get("status"), status);
        }
    }

    public static Specification<Product> inStock(Boolean inStock) {
        if(inStock == null) {
            return (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
        } else if(inStock) {
            return (root, _, criteriaBuilder) -> criteriaBuilder
                    .greaterThanOrEqualTo(root.get("quantity"), 1);
        } else {
            return (root, _, criteriaBuilder) -> criteriaBuilder
                    .equal(root.get("quantity"), 0);
        }
    }
    public static Specification<Product> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return cb.conjunction();
            if (minPrice != null && maxPrice == null) return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            if (minPrice == null) return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
            return cb.between(root.get("price"), minPrice, maxPrice);
        };
    }
    public static Specification<Product> hasCategory(String categoryId) {
        if(StringUtils.isBlank(categoryId)) {
            return (_, _, criteriaBuilder) -> criteriaBuilder.conjunction();
        } else {
            return (root, query, criteriaBuilder) -> {
                Join<Product, Category> categoryJoin = root.join("category");
                return criteriaBuilder.equal(categoryJoin.get("id"), categoryId);
            };
        }
    }
}
