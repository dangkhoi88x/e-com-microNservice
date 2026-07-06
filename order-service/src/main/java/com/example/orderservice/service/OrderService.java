package com.example.orderservice.service;

import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.PageResponse;

public interface OrderService {
    OrderResponse createOrder(String userId, CreateOrderRequest request);

    PageResponse<OrderResponse> getMyOrders(String userId, int page, int size);

    PageResponse<OrderResponse> getAllOrders(int page, int size);

    OrderResponse getOrderDetail(String userId, String orderId);

    OrderResponse updateOrderStatus(String orderId, OrderStatus status);

    OrderResponse cancelOrder(String userId, String orderId);
}
