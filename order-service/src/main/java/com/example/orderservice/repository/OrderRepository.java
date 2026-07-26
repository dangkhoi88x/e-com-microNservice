package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable pageable);
    Page<Order> findBySellerId(String sellerId, Pageable pageable);
    List<Order> findBySellerIdAndCreatedAtBetween(String sellerId, Instant from, Instant to);
    List<Order> findByCreatedAtBetween(Instant from, Instant to);

    Page<Order> findByPromotionCodeIgnoreCase(String promotionCode, Pageable pageable);

    Optional<Order> findByIdAndUserId(String id, String userId);

    boolean existsByOrderCode(String orderCode);

    Optional<Order> findByOrderCodeAndUserId(String orderCode, String userId);
    Optional<Order> findByOrderCodeIgnoreCase(String orderCode);


}
