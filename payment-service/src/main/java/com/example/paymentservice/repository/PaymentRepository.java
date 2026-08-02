package com.example.paymentservice.repository;

import com.example.paymentservice.common.PaymentStatus;
import com.example.paymentservice.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,String> {
    Page<Payment> findByUserId(String userId, Pageable pageable);
    Optional<Payment> findByIdAndUserId(String id, String userId);
    //Khóa Payment
    //hai request đồng thời không tạo hai Stripe Checkout Session cho một Payment
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.id = :id and payment.userId = :userId")
    Optional<Payment> findByIdAndUserIdForUpdate(@Param("id") String id, @Param("userId") String userId);

    Optional<Payment> findByOrderIdAndUserId(String orderId, String userId);
    Optional<Payment> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.stripeCheckoutSessionId = :sessionId")
    Optional<Payment> findByStripeCheckoutSessionIdForUpdate(@Param("sessionId") String sessionId);

    boolean existsByOrderIdAndStatus(String orderId, PaymentStatus status);
    boolean existsByOrderIdAndUserIdAndStatusIn(String orderId, String userId, Collection<PaymentStatus> statuses);
}
