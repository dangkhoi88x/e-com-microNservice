package com.example.sellerservice.repository;

import com.example.sellerservice.entity.SellerShop;
import com.example.sellerservice.entity.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerShopRepository extends JpaRepository<SellerShop, UUID> {
    Optional<SellerShop> findByOwnerUserId(String ownerUserId);

    Page<SellerShop> findAllByStatusOrderByCreatedAtDesc(SellerStatus status, Pageable pageable);

    boolean existsBySlug(String slug);
}
