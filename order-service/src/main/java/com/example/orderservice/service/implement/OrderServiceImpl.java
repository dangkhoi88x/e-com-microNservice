package com.example.orderservice.service.implement;


import com.example.event.OrderCancelledEvent;
import com.example.event.OrderCreatedEvent;
import com.example.event.OrderItemEvent;
import com.example.event.OrderStatusUpdatedEvent;
import com.example.event.CodPaymentCreatedEvent;
import com.example.event.PaymentCancelledEvent;
import com.example.event.PaymentFailedEvent;
import com.example.event.PaymentSuccessEvent;
import com.example.orderservice.client.InventoryClient;
import com.example.orderservice.client.CartClient;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.client.PromotionClient;
import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.CheckoutOrderRequest;
import com.example.orderservice.dto.request.InventoryOrderRequest;
import com.example.orderservice.dto.request.ReserveInventoryItemRequest;
import com.example.orderservice.dto.request.ReserveInventoryRequest;
import com.example.orderservice.dto.response.OrderItemResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.PageResponse;
import com.example.orderservice.dto.response.ProductDetailResponse;
import com.example.orderservice.dto.response.ProductVariantResponse;
import com.example.orderservice.dto.response.PromotionCalculationResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.exception.ErrorCode;
import com.example.orderservice.exception.OrderServiceException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-SERVICE")
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled";
    private static final String ORDER_STATUS_UPDATED_TOPIC = "order-status-updated";
    private static final String COD_METHOD = "COD";
    private static final DateTimeFormatter ORDER_CODE_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.PENDING_PAYMENT,
            OrderStatus.CONFIRMED
    );

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final CartClient cartClient;
    private final PromotionClient promotionClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request, String token) {
        return createOrder(userId, request, token, null);
    }

    private OrderResponse createOrder(String userId, CreateOrderRequest request, String token, String campaignCode) {
        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .userId(userId)
                .shippingAddress(request.shippingAddress())
                .status(OrderStatus.PENDING)
                .subtotalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (var itemRequest : request.items()) {
            ProductDetailResponse product = productClient.getProductById(itemRequest.productId());

            if (product == null) {
                throw new OrderServiceException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            if (!"ACTIVE".equals(product.status())) {
                throw new OrderServiceException(ErrorCode.PRODUCT_NOT_ACTIVE);
            }

            ProductVariantResponse variant = findVariant(product, itemRequest.variantId());
            BigDecimal itemPrice = variant != null ? variant.price() : product.price();
            String itemName = variant != null
                    ? product.name() + " - " + formatVariantAttributes(variant)
                    : product.name();

            BigDecimal subtotal = itemPrice
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.id())
                    .variantId(variant != null ? variant.id() : null)
                    .productName(itemName)
                    .price(itemPrice)
                    .quantity(itemRequest.quantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setSubtotalAmount(totalAmount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(totalAmount);

        if (hasPromotion(campaignCode)) {
            PromotionCalculationResponse promotion = promotionClient.validate(campaignCode, totalAmount);
            order.setPromotionCode(promotion.campaignCode());
            order.setDiscountAmount(promotion.discountAmount());
            order.setTotalAmount(promotion.finalAmount());
        }

        Order savedOrder = orderRepository.saveAndFlush(order);
        OrderStatus oldStatus = savedOrder.getStatus();
        boolean inventoryReserved = false;

        try {
            reserveInventory(savedOrder, token);
            inventoryReserved = true;
            if (hasPromotion(savedOrder.getPromotionCode())) {
                promotionClient.reserve(savedOrder.getPromotionCode(), userId, savedOrder.getId(), savedOrder.getSubtotalAmount());
            }
            savedOrder.setStatus(OrderStatus.PENDING_PAYMENT);
            Order reservedOrder = orderRepository.save(savedOrder);
            publishOrderStatusUpdatedEvent(reservedOrder, oldStatus);
            publishOrderCreatedEvent(reservedOrder);
            return toOrderResponse(reservedOrder);
        } catch (RuntimeException exception) {
            log.error("Failed to prepare checkout resources: orderId={}", savedOrder.getId(), exception);
            if (inventoryReserved) {
                safeReleaseInventory(savedOrder, token);
                safeReleasePromotion(savedOrder);
            }
            savedOrder.setStatus(inventoryReserved && hasPromotion(savedOrder.getPromotionCode())
                    ? OrderStatus.PROMOTION_FAILED
                    : OrderStatus.INVENTORY_FAILED);
            Order failedOrder = orderRepository.save(savedOrder);
            publishOrderStatusUpdatedEvent(failedOrder, oldStatus);

            return toOrderResponse(failedOrder);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(String userId, int page, int size) {
        Pageable pageable = createOrderPageable(page, size);
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(this::toOrderResponse)
                .toList();

        return toPageResponse(orderPage, content);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = createOrderPageable(page, size);
        Page<Order> orderPage = orderRepository.findAll(pageable);

        List<OrderResponse> content = orderPage.getContent()
                .stream()
                .map(this::toOrderResponse)
                .toList();

        return toPageResponse(orderPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        return toOrderResponse(order);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetailForAdmin(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        return toOrderResponse(order);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public OrderResponse updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String userId, String orderId, String token) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new OrderServiceException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            releaseInventory(order, token);
            safeReleasePromotion(order);
            cartClient.release(order.getId());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        publishOrderCancelledEvent(savedOrder);
        return toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public void startShippingFromCodPayment(CodPaymentCreatedEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.SHIPPING || order.getStatus() == OrderStatus.COMPLETED) {
            log.info("Skip COD shipping transition because order is already progressed: orderId={}, paymentId={}, status={}",
                    event.getOrderId(), event.getPaymentId(), order.getStatus());
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Skip COD shipping transition because order is not pending payment: orderId={}, paymentId={}, status={}",
                    event.getOrderId(), event.getPaymentId(), order.getStatus());
            return;
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.SHIPPING);
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);

        log.info("Order is shipping for COD payment: orderId={}, paymentId={}",
                event.getOrderId(), event.getPaymentId());
    }

    @Override
    @Transactional
    public void confirmOrderFromPaymentSuccess(PaymentSuccessEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        OrderStatus targetStatus = COD_METHOD.equals(event.getMethod())
                ? OrderStatus.COMPLETED
                : OrderStatus.CONFIRMED;

        if (order.getStatus() == targetStatus) {
            finalizePaymentSuccess(order);
            log.info("Skip payment success because order is already {}: orderId={}, paymentId={}",
                    targetStatus,
                    event.getOrderId(),
                    event.getPaymentId());
            return;
        }

        boolean codOrderReady = COD_METHOD.equals(event.getMethod())
                && (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.SHIPPING);
        boolean onlineOrderReady = !COD_METHOD.equals(event.getMethod())
                && order.getStatus() == OrderStatus.PENDING_PAYMENT;
        if (!codOrderReady && !onlineOrderReady) {
            log.warn("Skip payment success because order is not ready for completion: orderId={}, paymentId={}, status={}",
                    event.getOrderId(),
                    event.getPaymentId(),
                    order.getStatus());
            return;
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(targetStatus);
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);
        finalizePaymentSuccess(savedOrder);

        log.info("Order {} from payment success: orderId={}, paymentId={}", targetStatus,
                event.getOrderId(),
                event.getPaymentId());
    }

    @Override
    @Transactional
    public void cancelOrderFromPaymentFailed(PaymentFailedEvent event) {
        cancelOrderFromPaymentEvent(event.getOrderId(), event.getPaymentId(), "failed");
    }

    @Override
    @Transactional
    public void cancelOrderFromPaymentCancelled(PaymentCancelledEvent event) {
        cancelOrderFromPaymentEvent(event.getOrderId(), event.getPaymentId(), "cancelled");
    }

    private void cancelOrderFromPaymentEvent(String orderId, String paymentId, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            releasePaymentReservations(order);
            log.info("Skip payment {} because order is already cancelled: orderId={}, paymentId={}",
                    paymentStatus,
                    orderId,
                    paymentId);
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Skip payment {} because order is not pending payment: orderId={}, paymentId={}, status={}",
                    paymentStatus,
                    orderId,
                    paymentId,
                    order.getStatus());
            return;
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        publishOrderStatusUpdatedEvent(savedOrder, oldStatus);
        publishOrderCancelledEvent(savedOrder);
        releasePaymentReservations(savedOrder);

        log.info("Order cancelled from payment {}: orderId={}, paymentId={}",
                paymentStatus,
                orderId,
                paymentId);
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getVariantId(),
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUserId(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getPromotionCode(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getShippingAddress(),
                items,
                order.getCreatedAt()
        );
    }

    private Pageable createOrderPageable(int page, int size) {
        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        return PageRequest.of(currentPage - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<OrderResponse> toPageResponse(Page<Order> orderPage, List<OrderResponse> content) {
        return PageResponse.<OrderResponse>builder()
                .currentPage(orderPage.getNumber() + 1)
                .pageSize(orderPage.getSize())
                .totalPages(orderPage.getTotalPages())
                .totalElements(orderPage.getTotalElements())
                .content(content)
                .build();
    }

    private void publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .items(order.getItems()
                        .stream()
                        .map(item -> OrderItemEvent.builder()
                                .productId(item.getProductId())
                                .variantId(item.getVariantId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .build();

        kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish OrderCreatedEvent: orderId={}", order.getId(), throwable);
                        return;
                    }

                    log.info("Published OrderCreatedEvent: orderId={}", order.getId());
                });
    }

    private void reserveInventory(Order order, String token) {
        ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest(
                order.getId(),
                order.getItems()
                        .stream()
                        .map(item -> new ReserveInventoryItemRequest(
                                item.getProductId(),
                                item.getVariantId(),
                                item.getQuantity()
                        ))
                        .toList()
        );

        inventoryClient.reserveInventory(reserveRequest, token);
    }

    private void releaseInventory(Order order, String token) {
        inventoryClient.releaseInventory(new InventoryOrderRequest(order.getId()), token);
    }

    private boolean hasPromotion(String promotionCode) {
        return promotionCode != null && !promotionCode.isBlank();
    }

    private void safeReleaseInventory(Order order, String token) {
        try {
            releaseInventory(order, token);
        } catch (RuntimeException releaseException) {
            log.error("Failed to compensate inventory reservation: orderId={}", order.getId(), releaseException);
        }
    }

    private void safeReleasePromotion(Order order) {
        if (!hasPromotion(order.getPromotionCode())) return;
        try {
            promotionClient.release(order.getId());
        } catch (RuntimeException releaseException) {
            log.error("Failed to release promotion reservation: orderId={}", order.getId(), releaseException);
        }
    }

    /**
     * All downstream operations are idempotent by orderId. Re-running a Kafka
     * payment-success record therefore completes any previously interrupted step.
     */
    private void finalizePaymentSuccess(Order order) {
        if (hasPromotion(order.getPromotionCode())) {
            promotionClient.confirm(order.getId());
        }
        inventoryClient.confirmInventory(new InventoryOrderRequest(order.getId()));
        cartClient.finalize(order.getId());
    }

    /**
     * Failed/cancelled payment returns every reservation and unlocks only the
     * cart items that belong to this order's checkout session.
     */
    private void releasePaymentReservations(Order order) {
        if (hasPromotion(order.getPromotionCode())) {
            promotionClient.release(order.getId());
        }
        inventoryClient.releaseInventory(new InventoryOrderRequest(order.getId()));
        cartClient.release(order.getId());
    }

    @Override
    public OrderResponse checkout(String userId, CheckoutOrderRequest request, String token) {
        List<CartClient.CartItem> items = cartClient.checkoutItems(userId);
        if (items.isEmpty()) throw new OrderServiceException(ErrorCode.CART_CHECKOUT_EMPTY);
        OrderResponse order = createOrder(userId, new CreateOrderRequest(request.shippingAddress(), items.stream()
                .map(item -> new com.example.orderservice.dto.request.OrderItemRequest(item.productId(), item.variantId(), item.quantity())).toList()), token, request.campaignCode());
        if (OrderStatus.PENDING_PAYMENT.name().equals(order.status())) {
            cartClient.mark(userId, order.id(), items.stream().map(CartClient.CartItem::id).toList());
        }
        return order;
    }

    private ProductVariantResponse findVariant(ProductDetailResponse product, String variantId) {
        if (variantId == null || variantId.isBlank()) {
            return null;
        }

        if (product.variants() == null) {
            throw new OrderServiceException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        ProductVariantResponse variant = product.variants()
                .stream()
                .filter(item -> variantId.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new OrderServiceException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!"ACTIVE".equals(variant.status())) {
            throw new OrderServiceException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }

        return variant;
    }

    private String formatVariantAttributes(ProductVariantResponse variant) {
        if (variant.attributes() == null || variant.attributes().isEmpty()) {
            return variant.sku();
        }

        return String.join(", ", variant.attributes().values());
    }

    private void publishOrderCancelledEvent(Order order) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .cancelledAt(Instant.now())
                .build();

        kafkaTemplate.send(ORDER_CANCELLED_TOPIC, order.getId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish OrderCancelledEvent: orderId={}", order.getId(), throwable);
                        return;
                    }

                    log.info("Published OrderCancelledEvent: orderId={}", order.getId());
                });
    }

    private void publishOrderStatusUpdatedEvent(Order order, OrderStatus oldStatus) {
        OrderStatus newStatus = order.getStatus();
        OrderStatusUpdatedEvent event = OrderStatusUpdatedEvent.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .updatedAt(Instant.now())
                .build();

        kafkaTemplate.send(ORDER_STATUS_UPDATED_TOPIC, order.getId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to publish OrderStatusUpdatedEvent: orderId={}, oldStatus={}, newStatus={}",
                                order.getId(),
                                oldStatus,
                                newStatus,
                                throwable);
                        return;
                    }

                    log.info("Published OrderStatusUpdatedEvent: orderId={}, oldStatus={}, newStatus={}",
                            order.getId(),
                            oldStatus,
                            newStatus);
                });
    }

    private String generateOrderCode() {
        String orderCode;
        do {
            String date = ORDER_CODE_DATE_FORMAT.format(Instant.now());
            String suffix = java.util.UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
            orderCode = "ORD-" + date + "-" + suffix;
        } while (orderRepository.existsByOrderCode(orderCode));
        return orderCode;
    }
}
