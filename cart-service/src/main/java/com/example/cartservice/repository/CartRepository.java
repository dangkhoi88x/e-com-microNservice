package com.example.cartservice.repository;

import com.example.cartservice.entity.Cart;
import com.example.cartservice.enums.CartStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, java.util.UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByUserIdAndStatus(String userId, CartStatus status);
}
