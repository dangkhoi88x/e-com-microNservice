package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByPromotionCodeIgnoreCase(String promotionCode, Pageable pageable);

    Optional<Order> findByIdAndUserId(String id, String userId);

    boolean existsByOrderCode(String orderCode);

    Optional<Order> findByOrderCodeAndUserId(String orderCode, String userId);


}
