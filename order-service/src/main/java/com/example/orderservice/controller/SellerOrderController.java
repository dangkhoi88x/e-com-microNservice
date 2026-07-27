package com.example.orderservice.controller;

import com.example.orderservice.common.OrderStatus;
import com.example.orderservice.dto.response.ApiResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.PageResponse;
import com.example.orderservice.dto.response.SellerOrderDetailResponse;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders/seller")
@PreAuthorize("hasAuthority('ROLE_SELLER')")
public class SellerOrderController {
    private final OrderService orderService;
    @GetMapping public ApiResponse<PageResponse<OrderResponse>> getMine(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int size) { return body(HttpStatus.OK, orderService.getSellerOrders(jwt.getSubject(), page, size)); }
    @GetMapping("/{id}") public ApiResponse<SellerOrderDetailResponse> detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) { return body(HttpStatus.OK, orderService.getSellerOrderDetail(jwt.getSubject(), id)); }
    @PutMapping("/{id}/status") public ApiResponse<OrderResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String id, @RequestParam OrderStatus status) { return body(HttpStatus.OK, orderService.updateSellerOrderStatus(jwt.getSubject(), id, status)); }
    private <T> ApiResponse<T> body(HttpStatus status, T data) { return ApiResponse.<T>builder().status(status.value()).message("Seller order processed successfully").data(data).build(); }
}
