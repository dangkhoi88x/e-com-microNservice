package com.example.paymentservice.service.implement;

import com.example.paymentservice.client.OrderClient;
import com.example.paymentservice.common.PaymentMethod;
import com.example.paymentservice.common.PaymentStatus;
import com.example.paymentservice.dto.request.CreatePaymentRequest;
import com.example.paymentservice.dto.response.OrderResponse;
import com.example.paymentservice.exception.ErrorCode;
import com.example.paymentservice.exception.PaymentServiceException;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void createPaymentMapsConcurrentPendingPaymentConflictToDomainError() {
        String userId = "user-1";
        String orderId = "order-1";

        when(orderClient.getOrderDetail(eq(orderId), any()))
                .thenReturn(new OrderResponse(
                        orderId,
                        userId,
                        BigDecimal.valueOf(100_000),
                        "PENDING_PAYMENT",
                        "Ho Chi Minh City",
                        List.of(),
                        Instant.now()
                ));
        when(paymentRepository.existsByOrderIdAndUserIdAndStatusIn(
                eq(orderId), eq(userId), eq(List.of(PaymentStatus.PENDING))))
                .thenReturn(false);
        when(paymentRepository.existsByOrderIdAndUserIdAndStatusIn(
                eq(orderId), eq(userId), eq(List.of(PaymentStatus.SUCCESS))))
                .thenReturn(false);
        when(paymentRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_payments_one_pending_per_order"));

        PaymentServiceException exception = assertThrows(
                PaymentServiceException.class,
                () -> paymentService.createPayment(
                        userId,
                        "access-token",
                        new CreatePaymentRequest(orderId, PaymentMethod.COD)
                )
        );

        assertEquals(ErrorCode.PAYMENT_ALREADY_EXISTS, exception.getErrorCode());
    }
}
