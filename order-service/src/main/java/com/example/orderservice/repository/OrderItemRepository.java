package com.example.orderservice.repository;

import com.example.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    @Query("select item from OrderItem item join fetch item.order where item.id = :itemId")
    Optional<OrderItem> findByIdWithOrder(@Param("itemId") String itemId);
}
