package com.example.paymentservice.service.implement;

import com.example.event.PaymentCancelledEvent;
import com.example.event.CodPaymentCreatedEvent;
import com.example.event.PaymentFailedEvent;
import com.example.event.PaymentSuccessEvent;
import com.example.paymentservice.common.PaymentMethod;
import com.example.paymentservice.client.OrderClient;
import com.example.paymentservice.common.PaymentStatus;
import com.example.paymentservice.configuration.StripeProperties;
import com.example.paymentservice.dto.request.CreatePaymentRequest;
import com.example.paymentservice.dto.response.OrderResponse;
import com.example.paymentservice.dto.response.PageResponse;
import com.example.paymentservice.dto.response.PaymentResponse;
import com.example.paymentservice.dto.response.StripeCheckoutResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.exception.ErrorCode;
import com.example.paymentservice.exception.PaymentServiceException;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYMENT-SERVICE")
public class PaymentServiceImpl implements PaymentService {

    private static final String PAYMENT_CANCELLED_TOPIC = "payment-cancelled";
    private static final String COD_PAYMENT_CREATED_TOPIC = "payment-cod-created";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";
    private static final String PAYMENT_SUCCESS_TOPIC = "payment-success";

    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StripeProperties stripeProperties;

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

        Payment savedPayment;
        try {
            // The database partial unique index is the final protection when
            // concurrent requests both pass the exists(...) checks above.
            savedPayment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException exception) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }
        log.info("Payment created successfully: id={}, orderId={}", savedPayment.getId(), savedPayment.getOrderId());

        if (savedPayment.getMethod() == PaymentMethod.COD) {
            publishCodPaymentCreatedEvent(savedPayment);
        }

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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public PaymentResponse markPaymentSuccess(String userId, String token, String paymentId) {
        Payment payment = getPaymentForAdmin(paymentId);

        if (payment.getMethod() == PaymentMethod.STRIPE) {
            throw new PaymentServiceException(ErrorCode.STRIPE_REQUIRES_WEBHOOK);
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_ALREADY_SUCCESS);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_CANNOT_BE_COMPLETED);
        }

        // A COD/admin confirmation must not turn a payment into SUCCESS after
        // its order was cancelled or otherwise left the payment stage.
        OrderResponse order = orderClient.getOrderDetailForAdmin(payment.getOrderId(), token);
        validateOrderCompletable(payment, order);

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setFailureReason(null);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment marked success: id={}, orderId={}", savedPayment.getId(), savedPayment.getOrderId());
        publishPaymentSuccessEvent(savedPayment);

        return PaymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public PaymentResponse markPaymentFailed(String userId, String paymentId) {
        Payment payment = getPaymentForAdmin(paymentId);

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
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public PaymentResponse cancelPayment(String userId, String paymentId) {
        Payment payment = getPaymentForAdmin(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_CANNOT_BE_CANCELLED);
        }

        payment.setStatus(PaymentStatus.CANCELLED);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment cancelled: id={}, orderId={}", savedPayment.getId(), savedPayment.getOrderId());
        publishPaymentCancelledEvent(savedPayment);

        return PaymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional
    public StripeCheckoutResponse createStripeCheckout(String userId, String paymentId) {
        validateStripeConfiguration(false);

        Payment payment = paymentRepository.findByIdAndUserIdForUpdate(paymentId, userId)
                .orElseThrow(() -> new PaymentServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getMethod() != PaymentMethod.STRIPE) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_NOT_STRIPE);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_CANNOT_BE_COMPLETED);
        }

        if (StringUtils.hasText(payment.getStripeCheckoutUrl())
                && payment.getStripeCheckoutExpiresAt() != null
                && payment.getStripeCheckoutExpiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return toStripeCheckoutResponse(payment);
        }

        long stripeAmount = toStripeMinorUnit(payment.getAmount());
        String successUrl = stripeProperties.successUrl()
                .replace("{PAYMENT_ID}", payment.getId());
        String cancelUrl = stripeProperties.cancelUrl()
                .replace("{PAYMENT_ID}", payment.getId());

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(payment.getId())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("paymentId", payment.getId())
                .putMetadata("orderId", payment.getOrderId())
                .putMetadata("transactionCode", payment.getTransactionCode())
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("paymentId", payment.getId())
                                .putMetadata("orderId", payment.getOrderId())
                                .putMetadata("transactionCode", payment.getTransactionCode())
                                .build()
                )
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(stripeProperties.currency().toLowerCase())
                                                .setUnitAmount(stripeAmount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("NovaShop order " + payment.getTransactionCode())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(stripeProperties.secretKey())
                    .build();
            Session session = Session.create(params, requestOptions);

            payment.setStripeCheckoutSessionId(session.getId());
            payment.setStripeCheckoutUrl(session.getUrl());
            payment.setStripeCheckoutExpiresAt(
                    session.getExpiresAt() == null ? Instant.now().plusSeconds(1800) : Instant.ofEpochSecond(session.getExpiresAt())
            );
            Payment savedPayment = paymentRepository.save(payment);

            log.info("Stripe Checkout Session created: paymentId={}, orderId={}, sessionId={}",
                    savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getStripeCheckoutSessionId());
            return toStripeCheckoutResponse(savedPayment);
        } catch (StripeException exception) {
            log.error("Stripe Checkout Session creation failed: paymentId={}, orderId={}",
                    payment.getId(), payment.getOrderId(), exception);
            throw new PaymentServiceException(ErrorCode.STRIPE_CHECKOUT_FAILED);
        }
    }

    @Override
    @Transactional
    public PaymentResponse reconcileStripePayment(String userId, String paymentId) {
        validateStripeConfiguration(false);

        Payment payment = paymentRepository.findByIdAndUserIdForUpdate(paymentId, userId)
                .orElseThrow(() -> new PaymentServiceException(ErrorCode.PAYMENT_NOT_FOUND));

        return reconcileStripePayment(payment);
    }

    private PaymentResponse reconcileStripePayment(Payment payment) {

        if (payment.getMethod() != PaymentMethod.STRIPE) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_NOT_STRIPE);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return PaymentMapper.toResponse(payment);
        }
        if (!StringUtils.hasText(payment.getStripeCheckoutSessionId())) {
            throw new PaymentServiceException(ErrorCode.PAYMENT_CANNOT_BE_COMPLETED);
        }

        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(stripeProperties.secretKey())
                    .build();
            Session session = Session.retrieve(payment.getStripeCheckoutSessionId(), requestOptions);

            log.info("Stripe reconciliation lookup: paymentId={}, sessionId={}, status={}, paymentStatus={}",
                    payment.getId(), session.getId(), session.getStatus(), session.getPaymentStatus());

            if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
                return PaymentMapper.toResponse(payment);
            }

            Payment savedPayment = completeStripePayment(payment, session, null);
            log.info("Reconciled paid Stripe Checkout Session: paymentId={}, orderId={}, sessionId={}",
                    savedPayment.getId(), savedPayment.getOrderId(), session.getId());
            return PaymentMapper.toResponse(savedPayment);
        } catch (PaymentServiceException exception) {
            log.warn("Stripe reconciliation rejected: paymentId={}, orderId={}, reason={}",
                    payment.getId(), payment.getOrderId(), exception.getErrorCode().name());
            throw exception;
        } catch (StripeException exception) {
            log.error("Stripe reconciliation failed: paymentId={}, orderId={}", payment.getId(), payment.getOrderId(), exception);
            throw new PaymentServiceException(ErrorCode.STRIPE_CHECKOUT_FAILED);
        }
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String signature) {
        validateStripeConfiguration(true);

        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException | RuntimeException exception) {
            log.warn("Rejected Stripe webhook with invalid payload or signature");
            throw new PaymentServiceException(ErrorCode.INVALID_STRIPE_WEBHOOK);
        }

        if (!isSupportedStripeCheckoutEvent(event.getType())) {
            log.debug("Ignoring Stripe event: id={}, type={}", event.getId(), event.getType());
            return;
        }

        StripeObject stripeObject = deserializeStripeEventObject(event);
        if (!(stripeObject instanceof Session session)) {
            log.warn("Stripe event does not contain a Checkout Session: id={}, type={}", event.getId(), event.getType());
            throw new PaymentServiceException(ErrorCode.INVALID_STRIPE_WEBHOOK);
        }

        if (isStripePaymentCompletedEvent(event.getType())
                && !"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            log.info("Stripe Checkout Session is not paid yet: eventId={}, sessionId={}, paymentStatus={}",
                    event.getId(), session.getId(), session.getPaymentStatus());
            return;
        }

        Payment payment = paymentRepository.findByStripeCheckoutSessionIdForUpdate(session.getId())
                .orElse(null);
        if (payment == null) {
            log.warn("Ignoring Stripe event for unknown Checkout Session: eventId={}, sessionId={}",
                    event.getId(), session.getId());
            return;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Stripe event already applied: eventId={}, paymentId={}", event.getId(), payment.getId());
            return;
        }
        if (payment.getMethod() != PaymentMethod.STRIPE) {
            log.warn("Ignoring Stripe event for a non-Stripe payment: eventId={}, paymentId={}, method={}",
                    event.getId(), payment.getId(), payment.getMethod());
            return;
        }

        if (isStripeCheckoutExpiredEvent(event.getType())) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setFailureReason("Stripe Checkout Session expired");
            payment.setStripeEventId(event.getId());
            Payment savedPayment = paymentRepository.save(payment);
            publishPaymentCancelledEvent(savedPayment);
            log.info("Expired Stripe Checkout Session cancelled payment: eventId={}, paymentId={}, orderId={}",
                    event.getId(), savedPayment.getId(), savedPayment.getOrderId());
            return;
        }

        if (isStripeAsyncPaymentFailedEvent(event.getType())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Stripe asynchronous payment failed");
            payment.setStripeEventId(event.getId());
            Payment savedPayment = paymentRepository.save(payment);
            publishPaymentFailedEvent(savedPayment);
            log.info("Stripe asynchronous payment failed: eventId={}, paymentId={}, orderId={}",
                    event.getId(), savedPayment.getId(), savedPayment.getOrderId());
            return;
        }

        Payment savedPayment = completeStripePayment(payment, session, event.getId());
        log.info("Stripe payment completed: eventId={}, paymentId={}, orderId={}",
                event.getId(), savedPayment.getId(), savedPayment.getOrderId());
    }

    private Payment completeStripePayment(Payment payment, Session session, String stripeEventId) {
        validateStripeCheckoutResult(payment, session);
        OrderResponse order = orderClient.getOrderDetailForPaymentWebhook(payment.getOrderId());
        validateOrderCompletable(payment, order);

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setFailureReason(null);
        payment.setStripePaymentIntentId(session.getPaymentIntent());
        payment.setStripeEventId(stripeEventId);
        payment.setPaidAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);
        publishPaymentSuccessEvent(savedPayment);
        return savedPayment;
    }

    private Payment getPaymentForUser(String userId, String paymentId) {
        return paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentServiceException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private Payment getPaymentForAdmin(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentServiceException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validateStripeConfiguration(boolean webhookRequired) {
        boolean missingSecretKey = !webhookRequired && !StringUtils.hasText(stripeProperties.secretKey());
        boolean missingWebhookSecret = webhookRequired && !StringUtils.hasText(stripeProperties.webhookSecret());
        if (missingSecretKey || missingWebhookSecret) {
            throw new PaymentServiceException(ErrorCode.STRIPE_NOT_CONFIGURED);
        }
    }

    private StripeObject deserializeStripeEventObject(Event event) {
        try {
            var deserializer = event.getDataObjectDeserializer();
            var stripeObject = deserializer.getObject();
            if (stripeObject.isPresent()) {
                return stripeObject.get();
            }

            // Stripe can deliver an event using the API version pinned on the
            // webhook endpoint. We only consume stable Checkout Session fields
            // and validate all identifiers, amount and currency afterwards.
            log.warn("Stripe event API version differs from SDK; using guarded deserialization: eventId={}, type={}",
                    event.getId(), event.getType());
            return deserializer.deserializeUnsafe();
        } catch (Exception exception) {
            log.warn("Could not deserialize Stripe event: eventId={}, type={}", event.getId(), event.getType());
            throw new PaymentServiceException(ErrorCode.INVALID_STRIPE_WEBHOOK);
        }
    }

    private long toStripeMinorUnit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new PaymentServiceException(ErrorCode.STRIPE_CHECKOUT_FAILED);
        }

        try {
            // VND is a zero-decimal Stripe currency, therefore the backend
            // order total is sent directly without multiplying by 100.
            return amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException exception) {
            throw new PaymentServiceException(ErrorCode.STRIPE_CHECKOUT_FAILED);
        }
    }

    private StripeCheckoutResponse toStripeCheckoutResponse(Payment payment) {
        return new StripeCheckoutResponse(
                payment.getId(),
                payment.getStripeCheckoutSessionId(),
                payment.getStripeCheckoutUrl(),
                payment.getStripeCheckoutExpiresAt()
        );
    }

    private boolean isStripePaymentCompletedEvent(String eventType) {
        return "checkout.session.completed".equals(eventType)
                || "checkout.session.async_payment_succeeded".equals(eventType);
    }

    private boolean isStripeCheckoutExpiredEvent(String eventType) {
        return "checkout.session.expired".equals(eventType);
    }

    private boolean isStripeAsyncPaymentFailedEvent(String eventType) {
        return "checkout.session.async_payment_failed".equals(eventType);
    }

    private boolean isSupportedStripeCheckoutEvent(String eventType) {
        return isStripePaymentCompletedEvent(eventType)
                || isStripeCheckoutExpiredEvent(eventType)
                || isStripeAsyncPaymentFailedEvent(eventType);
    }

    private void validateStripeCheckoutResult(Payment payment, Session session) {
        String metadataPaymentId = session.getMetadata() == null ? null : session.getMetadata().get("paymentId");
        String metadataOrderId = session.getMetadata() == null ? null : session.getMetadata().get("orderId");
        Long amountTotal = session.getAmountTotal();
        String currency = session.getCurrency();

        boolean paid = "paid".equalsIgnoreCase(session.getPaymentStatus());
        boolean paymentMatches = payment.getId().equals(metadataPaymentId);
        boolean orderMatches = payment.getOrderId().equals(metadataOrderId);
        boolean amountMatches = amountTotal != null && amountTotal == toStripeMinorUnit(payment.getAmount());
        boolean currencyMatches = currency != null && currency.equalsIgnoreCase(stripeProperties.currency());

        if (!paid || !paymentMatches || !orderMatches || !amountMatches || !currencyMatches) {
            log.error("Stripe webhook validation failed: paymentId={}, sessionId={}, paid={}, paymentMatches={}, orderMatches={}, amountMatches={}, currencyMatches={}",
                    payment.getId(), session.getId(), paid, paymentMatches, orderMatches, amountMatches, currencyMatches);
            throw new PaymentServiceException(ErrorCode.INVALID_STRIPE_WEBHOOK);
        }
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

    private void validateOrderCompletable(Payment payment, OrderResponse order) {
        if (order == null) {
            throw new PaymentServiceException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (!payment.getUserId().equals(order.userId())) {
            throw new PaymentServiceException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        boolean codOrderReady = payment.getMethod() == PaymentMethod.COD
                && ("PENDING_PAYMENT".equals(order.status()) || "SHIPPING".equals(order.status()));
        boolean onlineOrderReady = payment.getMethod() != PaymentMethod.COD
                && "PENDING_PAYMENT".equals(order.status());

        if (!codOrderReady && !onlineOrderReady) {
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

    private void publishCodPaymentCreatedEvent(Payment payment) {
        CodPaymentCreatedEvent event = CodPaymentCreatedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .createdAt(Instant.now())
                .build();

        kafkaTemplate.send(COD_PAYMENT_CREATED_TOPIC, payment.getOrderId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish CodPaymentCreatedEvent: paymentId={}, orderId={}",
                                payment.getId(), payment.getOrderId(), throwable);
                        return;
                    }

                    log.info("Published CodPaymentCreatedEvent: paymentId={}, orderId={}",
                            payment.getId(), payment.getOrderId());
                });
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
