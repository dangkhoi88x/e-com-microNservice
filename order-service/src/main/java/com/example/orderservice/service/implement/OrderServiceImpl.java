package com.example.orderservice.service.implement;


import com.example.event.OrderCreatedEvent;
import com.example.orderservice.client.ProductClient;
import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.response.OrderItemResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.ProductDetailResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.exception.ErrorCode;
import com.example.orderservice.exception.OrderServiceException;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-SERVICE")
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_CREATED_TOPIC = "order-created";

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
    public List<OrderResponse> getMyOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Override
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

        order.setStatus(status);
        return toOrderResponse(orderRepository.save(order));
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

    private void publishOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .createdAt(order.getCreatedAt())
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
}
