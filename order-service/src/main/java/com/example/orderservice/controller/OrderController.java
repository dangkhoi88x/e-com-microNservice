package com.example.orderservice.controller;

import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.response.ApiResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.PageResponse;
import com.example.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CreateOrderRequest request
    ) {
        OrderResponse data = orderService.createOrder(jwt.getSubject(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<OrderResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Order created successfully")
                        .data(data)
                        .build());
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getAllOrders(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        PageResponse<OrderResponse> data = orderService.getAllOrders(page, size);

        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/my-orders")
    public ApiResponse<PageResponse<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        PageResponse<OrderResponse> data = orderService.getMyOrders(jwt.getSubject(), page, size);

        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        OrderResponse data = orderService.getOrderDetail(jwt.getSubject(), id);

        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(data)
                .build();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable String id,
            @RequestParam OrderStatus status
    ) {
        OrderResponse data = orderService.updateOrderStatus(id, status);

        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order status updated successfully")
                .data(data)
                .build();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        OrderResponse data = orderService.cancelOrder(jwt.getSubject(), id);

        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order cancelled successfully")
                .data(data)
                .build();
    }
}
