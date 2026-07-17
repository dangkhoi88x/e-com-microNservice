package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Optional<Inventory> findByProductId(String productId);

    Optional<Inventory> findByVariantId(String variantId);

    List<Inventory> findByProductIdIn(List<String> productIds);

    boolean existsByProductId(String productId);

    boolean existsByVariantId(String variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") String productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.variantId = :variantId")
    Optional<Inventory> findByVariantIdForUpdate(@Param("variantId") String variantId);
}
