package com.example.orderservice.service;

import com.example.event.PaymentCancelledEvent;
import com.example.event.CodPaymentCreatedEvent;
import com.example.event.PaymentFailedEvent;
import com.example.event.PaymentSuccessEvent;
import com.example.event.ShipmentStatusUpdatedEvent;
import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.CheckoutOrderRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.PageResponse;
import com.example.orderservice.dto.response.ReviewEligibilityResponse;

public interface OrderService {
    OrderResponse createOrder(String userId, CreateOrderRequest request, String token);
    OrderResponse checkout(String userId, CheckoutOrderRequest request, String token);

    PageResponse<OrderResponse> getMyOrders(String userId, int page, int size);

    PageResponse<OrderResponse> getAllOrders(int page, int size);

    PageResponse<OrderResponse> getOrdersByPromotionCode(String promotionCode, int page, int size);

    OrderResponse getOrderDetail(String userId, String orderId);

    OrderResponse getOrderDetailForAdmin(String orderId);

    OrderResponse updateOrderStatus(String orderId, OrderStatus status);

    OrderResponse cancelOrder(String userId, String orderId, String token);

    void startShippingFromCodPayment(CodPaymentCreatedEvent event);

    void confirmOrderFromPaymentSuccess(PaymentSuccessEvent event);

    void cancelOrderFromPaymentFailed(PaymentFailedEvent event);

    void cancelOrderFromPaymentCancelled(PaymentCancelledEvent event);

    void updateOrderFromShipment(ShipmentStatusUpdatedEvent event);

    ReviewEligibilityResponse checkReviewEligibility(String userId, String orderItemId);
}
