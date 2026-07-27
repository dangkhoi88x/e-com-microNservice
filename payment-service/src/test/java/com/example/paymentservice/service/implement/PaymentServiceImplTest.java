package com.example.paymentservice.service.implement;

import com.example.paymentservice.client.OrderClient;
import com.example.paymentservice.common.PaymentMethod;
import com.example.paymentservice.common.PaymentStatus;
import com.example.paymentservice.configuration.StripeProperties;
import com.example.paymentservice.dto.request.CreatePaymentRequest;
import com.example.paymentservice.dto.response.OrderResponse;
import com.example.paymentservice.entity.Payment;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private StripeProperties stripeProperties;

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

    @Test
    void adminCannotCompleteStripePaymentWithoutVerifiedWebhook() {
        Payment stripePayment = Payment.builder()
                .id("payment-1")
                .orderId("order-1")
                .userId("user-1")
                .amount(BigDecimal.valueOf(100_000))
                .method(PaymentMethod.STRIPE)
                .status(PaymentStatus.PENDING)
                .transactionCode("PAY-1")
                .build();
        when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(stripePayment));

        PaymentServiceException exception = assertThrows(
                PaymentServiceException.class,
                () -> paymentService.markPaymentSuccess("admin-1", "admin-token", "payment-1")
        );

        assertEquals(ErrorCode.STRIPE_REQUIRES_WEBHOOK, exception.getErrorCode());
    }

    @Test
    void verifiedStripeWebhookCompletesPaymentOnlyOnceWhenEventIsRetried() throws Exception {
        String paymentId = "payment-1";
        String orderId = "order-1";
        String sessionId = "cs_test_1";
        String webhookSecret = "whsec_test";
        long timestamp = Instant.now().getEpochSecond();
        String payload = """
                {
                  "id": "evt_test_1",
                  "object": "event",
                  "api_version": "2025-02-24.acacia",
                  "created": %d,
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "checkout.session",
                      "amount_total": 100000,
                      "currency": "vnd",
                      "metadata": {
                        "orderId": "%s",
                        "paymentId": "%s"
                      },
                      "payment_intent": "pi_test_1",
                      "payment_status": "paid"
                    }
                  },
                  "livemode": false,
                  "pending_webhooks": 1,
                  "type": "checkout.session.completed"
                }
                """.formatted(timestamp, sessionId, orderId, paymentId);
        String signature = stripeSignature(payload, webhookSecret, timestamp);

        Payment stripePayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId("user-1")
                .amount(BigDecimal.valueOf(100_000))
                .method(PaymentMethod.STRIPE)
                .status(PaymentStatus.PENDING)
                .transactionCode("PAY-1")
                .stripeCheckoutSessionId(sessionId)
                .build();
        OrderResponse order = new OrderResponse(
                orderId,
                "user-1",
                BigDecimal.valueOf(100_000),
                "PENDING_PAYMENT",
                "Ho Chi Minh City",
                List.of(),
                Instant.now()
        );

        when(stripeProperties.webhookSecret()).thenReturn(webhookSecret);
        when(stripeProperties.currency()).thenReturn("vnd");
        when(paymentRepository.findByStripeCheckoutSessionIdForUpdate(sessionId))
                .thenReturn(Optional.of(stripePayment));
        when(orderClient.getOrderDetailForPaymentWebhook(orderId)).thenReturn(order);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(eq("payment-success"), eq(orderId), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        paymentService.handleStripeWebhook(payload, signature);
        paymentService.handleStripeWebhook(payload, signature);

        assertEquals(PaymentStatus.SUCCESS, stripePayment.getStatus());
        assertEquals("pi_test_1", stripePayment.getStripePaymentIntentId());
        assertEquals("evt_test_1", stripePayment.getStripeEventId());
        verify(kafkaTemplate, times(1)).send(eq("payment-success"), eq(orderId), any());
    }

    @Test
    void stripeWebhookRejectsInvalidSignature() {
        when(stripeProperties.webhookSecret()).thenReturn("whsec_test");

        PaymentServiceException exception = assertThrows(
                PaymentServiceException.class,
                () -> paymentService.handleStripeWebhook("{}", "t=1,v1=invalid")
        );

        assertEquals(ErrorCode.INVALID_STRIPE_WEBHOOK, exception.getErrorCode());
    }

    @Test
    void expiredStripeCheckoutCancelsPendingPaymentOnlyOnce() throws Exception {
        String paymentId = "payment-expired";
        String orderId = "order-expired";
        String sessionId = "cs_test_expired";
        String webhookSecret = "whsec_test";
        long timestamp = Instant.now().getEpochSecond();
        String payload = """
                {
                  "id": "evt_test_expired",
                  "object": "event",
                  "api_version": "2026-06-24.dahlia",
                  "created": %d,
                  "data": {
                    "object": {
                      "id": "%s",
                      "object": "checkout.session",
                      "amount_total": 100000,
                      "currency": "vnd",
                      "metadata": {
                        "orderId": "%s",
                        "paymentId": "%s"
                      },
                      "payment_status": "unpaid"
                    }
                  },
                  "livemode": false,
                  "pending_webhooks": 1,
                  "type": "checkout.session.expired"
                }
                """.formatted(timestamp, sessionId, orderId, paymentId);
        String signature = stripeSignature(payload, webhookSecret, timestamp);

        Payment stripePayment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId("user-1")
                .amount(BigDecimal.valueOf(100_000))
                .method(PaymentMethod.STRIPE)
                .status(PaymentStatus.PENDING)
                .transactionCode("PAY-EXPIRED")
                .stripeCheckoutSessionId(sessionId)
                .build();

        when(stripeProperties.webhookSecret()).thenReturn(webhookSecret);
        when(paymentRepository.findByStripeCheckoutSessionIdForUpdate(sessionId))
                .thenReturn(Optional.of(stripePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(eq("payment-cancelled"), eq(orderId), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        paymentService.handleStripeWebhook(payload, signature);
        paymentService.handleStripeWebhook(payload, signature);

        assertEquals(PaymentStatus.CANCELLED, stripePayment.getStatus());
        assertEquals("Stripe Checkout Session expired", stripePayment.getFailureReason());
        assertEquals("evt_test_expired", stripePayment.getStripeEventId());
        verify(kafkaTemplate, times(1)).send(eq("payment-cancelled"), eq(orderId), any());
    }

    private String stripeSignature(String payload, String secret, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest);
    }
}
