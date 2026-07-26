package com.example.paymentservice.entity;

import com.example.paymentservice.common.PaymentMethod;
import com.example.paymentservice.common.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, unique = true)
    private String transactionCode;

    private String failureReason;

    @Column(unique = true)
    private String stripeCheckoutSessionId;

    @Column(length = 2048)
    private String stripeCheckoutUrl;

    private Instant stripeCheckoutExpiresAt;

    private String stripePaymentIntentId;

    private String stripeEventId;

    private Instant paidAt;
}
