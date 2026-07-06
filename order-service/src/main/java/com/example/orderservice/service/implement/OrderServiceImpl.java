package com.example.orderservice.service.implement;


import com.example.event.OrderCancelledEvent;
import com.example.event.OrderCreatedEvent;
import com.example.event.OrderItemEvent;
import com.example.event.OrderStatusUpdatedEvent;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.response.OrderItemResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.PageResponse;
import com.example.orderservice.dto.response.ProductDetailResponse;
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
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-SERVICE")
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_CANCELLED_TOPIC = "order-cancelled";
    private static final String ORDER_STATUS_UPDATED_TOPIC = "order-status-updated";
    private static final Set<OrderStatus> CANCELLABLE_STATUSES = Set.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED
    );

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        Order order = Order.builder()
                .userId(userId)
                .shippingAddress(request.shippingAddress())
                .status(OrderStatus.PENDING)
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

            if (product.quantity() < itemRequest.quantity()) {
                throw new OrderServiceException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }

            BigDecimal subtotal = product.price()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.id())
                    .productName(product.name())
                    .price(product.price())
                    .quantity(itemRequest.quantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        publishOrderCreatedEvent(savedOrder);
        return toOrderResponse(savedOrder);
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
    // @PreAuthorize("hasAnyAuthority('ADMIN')")
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
    public OrderResponse cancelOrder(String userId, String orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderServiceException(ErrorCode.ORDER_NOT_FOUND));

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new OrderServiceException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        publishOrderCancelledEvent(savedOrder);
        return toOrderResponse(savedOrder);
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
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
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .items(order.getItems()
                        .stream()
                        .map(item -> OrderItemEvent.builder()
                                .productId(item.getProductId())
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

    private void publishOrderCancelledEvent(Order order) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
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
}
