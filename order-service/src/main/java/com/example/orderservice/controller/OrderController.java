package com.example.orderservice.controller;

import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.CheckoutOrderRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Authorization") String authorization,
            @RequestBody @Valid CreateOrderRequest request
    ) {
        String token = authorization.replace("Bearer ", "");
        OrderResponse data = orderService.createOrder(jwt.getSubject(), request, token);
        String message = OrderStatus.INVENTORY_FAILED.name().equals(data.status())
                ? "Order created but inventory reservation failed"
                : "Order created successfully";

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<OrderResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message(message)
                        .data(data)
                        .build());
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Authorization") String authorization, @RequestBody @Valid CheckoutOrderRequest request) {
        OrderResponse data = orderService.checkout(jwt.getSubject(), request, authorization.replace("Bearer ", ""));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<OrderResponse>builder().status(HttpStatus.CREATED.value()).message("Checkout created successfully").data(data).build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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

    @GetMapping("/by-promotion")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<PageResponse<OrderResponse>> getOrdersByPromotion(
            @RequestParam String promotionCode,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        PageResponse<OrderResponse> data = orderService.getOrdersByPromotionCode(promotionCode, page, size);
        return ApiResponse.<PageResponse<OrderResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Orders using promotion retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<OrderResponse> searchOrderForAdmin(@RequestParam String query) {
        OrderResponse data = orderService.searchOrderForAdmin(query);
        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAuthority('ROLE_USER')")
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
    @PreAuthorize("hasAuthority('ROLE_USER')")
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

    @GetMapping("/{id}/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<OrderResponse> getOrderDetailForAdmin(@PathVariable String id) {
        OrderResponse data = orderService.getOrderDetailForAdmin(id);

        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .data(data)
                .build();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ApiResponse<OrderResponse> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("Authorization") String authorization,
            @PathVariable String id
    ) {
        String token = authorization.replace("Bearer ", "");
        OrderResponse data = orderService.cancelOrder(jwt.getSubject(), id, token);

        return ApiResponse.<OrderResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order cancelled successfully")
                .data(data)
                .build();
    }
}
