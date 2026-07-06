package com.example.orderservice.service;

import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(String userId, CreateOrderRequest request);

    List<OrderResponse> getMyOrders(String userId);

    OrderResponse getOrderDetail(String userId, String orderId);

    OrderResponse updateOrderStatus(String orderId, OrderStatus status);
}
