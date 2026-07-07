package com.example.paymentservice.mapper;

import com.example.paymentservice.dto.response.PaymentResponse;
import com.example.paymentservice.entity.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getTransactionCode(),
                payment.getFailureReason(),
                payment.getCreatedAt()
        );
    }
}
