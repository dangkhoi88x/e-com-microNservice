package com.example.paymentservice.service.implement;

import com.example.event.PaymentCancelledEvent;
import com.example.event.PaymentFailedEvent;
import com.example.event.PaymentSuccessEvent;
import com.example.paymentservice.client.OrderClient;
import com.example.paymentservice.common.PaymentStatus;
import com.example.paymentservice.dto.request.CreatePaymentRequest;
import com.example.paymentservice.dto.response.OrderResponse;
import com.example.paymentservice.dto.response.PageResponse;
import com.example.paymentservice.dto.response.PaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.exception.ErrorCode;
import com.example.paymentservice.exception.PaymentServiceException;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-SERVICE")
public class PaymentServiceImpl implements PaymentService {

    private static final String PAYMENT_CANCELLED_TOPIC = "payment-cancelled";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";
    private static final String PAYMENT_SUCCESS_TOPIC = "payment-success";

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public PaymentResponse createPayment(String userId, String token, CreatePaymentRequest request) {
        OrderResponse order = orderClient.getOrderDetail(request.orderId(), token);
        validateOrderPayable(userId, order);

        if (paymentRepository.existsByOrderIdAndUserIdAndStatusIn(
                request.orderId(),
                userId,
                List.of(PaymentStatus.PENDING)
        )) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        if (paymentRepository.existsByOrderIdAndUserIdAndStatusIn(
                request.orderId(),
                userId,
                List.of(PaymentStatus.SUCCESS)
        )) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_ALREADY_SUCCESS);
        }

        Payment payment = Payment.builder()
                .orderId(order.id())
                .userId(userId)
                .amount(order.totalAmount())
                .method(request.method())
                .status(PaymentStatus.PENDING)
                .transactionCode(generateTransactionCode())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created successfully: id={}, orderId={}", savedPayment.getId(), savedPayment.getOrderId());

        return PaymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getMyPayments(String userId, int page, int size) {
        Pageable pageable = createPaymentPageable(page, size);
        Page<Payment> paymentPage = paymentRepository.findByUserId(userId, pageable);

        List<PaymentResponse> content = paymentPage.getContent()
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();

        return toPageResponse(paymentPage, content);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getAllPayments(int page, int size) {
        Pageable pageable = createPaymentPageable(page, size);
        Page<Payment> paymentPage = paymentRepository.findAll(pageable);

        List<PaymentResponse> content = paymentPage.getContent()
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();

        return toPageResponse(paymentPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentDetail(String userId, String paymentId) {
        Payment payment = getPaymentForUser(userId, paymentId);
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse markPaymentSuccess(String userId, String paymentId) {
        Payment payment = getPaymentForUser(userId, paymentId);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_ALREADY_SUCCESS);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_CANNOT_BE_COMPLETED);
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setFailureReason(null);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment marked success: id={}, orderId={}", savedPayment.getId(), savedPayment.getOrderId());
        publishPaymentSuccessEvent(savedPayment);

        return PaymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse markPaymentFailed(String userId, String paymentId) {
        Payment payment = getPaymentForUser(userId, paymentId);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_ALREADY_SUCCESS);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_CANNOT_BE_FAILED);
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Payment failed");

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment marked failed: id={}, orderId={}", savedPayment.getId(), savedPayment.getOrderId());
        publishPaymentFailedEvent(savedPayment);

        return PaymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(String userId, String paymentId) {
        Payment payment = getPaymentForUser(userId, paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_CANNOT_BE_CANCELLED);
        }

        payment.setStatus(PaymentStatus.CANCELLED);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment cancelled: id={}, orderId={}", savedPayment.getId(), savedPayment.getOrderId());
        publishPaymentCancelledEvent(savedPayment);

        return PaymentMapper.toResponse(savedPayment);
    }

    private Payment getPaymentForUser(String userId, String paymentId) {
        return paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentServiceException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validateOrderPayable(String userId, OrderResponse order) {
        if (order == null) {
            throw new PaymentServiceException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (!userId.equals(order.userId())) {
            throw new PaymentServiceException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        if (!"PENDING_PAYMENT".equals(order.status())) {
            throw new PaymentServiceException(ErrorCode.ORDER_NOT_PAYABLE);
        }
    }

    private Pageable createPaymentPageable(int page, int size) {
        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        return PageRequest.of(currentPage - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<PaymentResponse> toPageResponse(Page<Payment> paymentPage, List<PaymentResponse> content) {
        return PageResponse.<PaymentResponse>builder()
                .currentPage(paymentPage.getNumber() + 1)
                .pageSize(paymentPage.getSize())
                .totalPages(paymentPage.getTotalPages())
                .totalElements(paymentPage.getTotalElements())
                .content(content)
                .build();
    }

    private String generateTransactionCode() {
        return "PAY-" + UUID.randomUUID();
    }

    private void publishPaymentFailedEvent(Payment payment) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .method(payment.getMethod().name())
                .transactionCode(payment.getTransactionCode())
                .failureReason(payment.getFailureReason())
                .failedAt(Instant.now())
                .build();

        kafkaTemplate.send(PAYMENT_FAILED_TOPIC, payment.getOrderId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish PaymentFailedEvent: paymentId={}, orderId={}",
                                payment.getId(),
                                payment.getOrderId(),
                                throwable);
                        return;
                    }

                    log.info("Published PaymentFailedEvent: paymentId={}, orderId={}",
                            payment.getId(),
                            payment.getOrderId());
                });
    }

    private void publishPaymentCancelledEvent(Payment payment) {
        PaymentCancelledEvent event = PaymentCancelledEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .method(payment.getMethod().name())
                .transactionCode(payment.getTransactionCode())
                .cancelledAt(Instant.now())
                .build();

        kafkaTemplate.send(PAYMENT_CANCELLED_TOPIC, payment.getOrderId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish PaymentCancelledEvent: paymentId={}, orderId={}",
                                payment.getId(),
                                payment.getOrderId(),
                                throwable);
                        return;
                    }

                    log.info("Published PaymentCancelledEvent: paymentId={}, orderId={}",
                            payment.getId(),
                            payment.getOrderId());
                });
    }

    private void publishPaymentSuccessEvent(Payment payment) {
        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .method(payment.getMethod().name())
                .transactionCode(payment.getTransactionCode())
                .paidAt(Instant.now())
                .build();

        kafkaTemplate.send(PAYMENT_SUCCESS_TOPIC, payment.getOrderId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish PaymentSuccessEvent: paymentId={}, orderId={}",
                                payment.getId(),
                                payment.getOrderId(),
                                throwable);
                        return;
                    }

                    log.info("Published PaymentSuccessEvent: paymentId={}, orderId={}",
                            payment.getId(),
                            payment.getOrderId());
                });
    }
}
