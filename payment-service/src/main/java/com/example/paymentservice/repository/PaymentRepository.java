package com.example.paymentservice.repository;

import com.example.paymentservice.common.PaymentStatus;
import com.example.paymentservice.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,String> {
    Page<Payment> findByUserId(String userId, Pageable pageable);
    Optional<Payment> findByIdAndUserId(String id, String userId);
    Optional<Payment> findByOrderIdAndUserId(String orderId, String userId);
    boolean existsByOrderIdAndStatus(String orderId, PaymentStatus status);
    boolean existsByOrderIdAndUserIdAndStatusIn(String orderId, String userId, Collection<PaymentStatus> statuses);
}
