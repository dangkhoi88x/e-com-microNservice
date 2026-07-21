package com.example.promotionservice.controller;

import com.example.promotionservice.dto.request.CreateFlashDealRequest;
import com.example.promotionservice.dto.response.*;
import com.example.promotionservice.service.FlashDealService;
import com.example.promotionservice.entity.FlashDealStatus;
import com.example.promotionservice.entity.SaleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/flash-deals")
public class FlashDealController {
    private final FlashDealService service;
    @PostMapping public ApiResponse<FlashDealResponse> create(@Valid @RequestBody CreateFlashDealRequest request) { return body(HttpStatus.CREATED, service.create(request)); }
    @GetMapping public ApiResponse<List<FlashDealResponse>> getAll(@RequestParam(required = false) String status) { return body(HttpStatus.OK, service.getAll(status)); }
    @GetMapping("/live") public ApiResponse<List<FlashDealResponse>> getLive() { return body(HttpStatus.OK, service.getByStatusAndType(FlashDealStatus.LIVE, SaleType.FLASH)); }
    @GetMapping("/upcoming") public ApiResponse<List<FlashDealResponse>> getUpcoming() { return body(HttpStatus.OK, service.getByStatusAndType(FlashDealStatus.SCHEDULED, SaleType.FLASH)); }
    @GetMapping("/long-term/live") public ApiResponse<List<FlashDealResponse>> getLongTermLive() { return body(HttpStatus.OK, service.getByStatusAndType(FlashDealStatus.LIVE, SaleType.LONG_TERM)); }
    @GetMapping("/active") public ApiResponse<List<FlashDealResponse>> getAllActiveSales() { return body(HttpStatus.OK, service.getByStatusAndType(FlashDealStatus.LIVE, null)); }
    @GetMapping("/{id}") public ApiResponse<FlashDealResponse> getById(@PathVariable String id) { return body(HttpStatus.OK, service.getById(id)); }
    @PutMapping("/{id}") public ApiResponse<FlashDealResponse> update(@PathVariable String id, @Valid @RequestBody CreateFlashDealRequest request) { return body(HttpStatus.OK, service.update(id, request)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable String id) { service.delete(id); return body(HttpStatus.OK, null); }
    @PostMapping("/reserve") public ApiResponse<List<FlashDealPriceResponse>> reserve(@Valid @RequestBody com.example.promotionservice.dto.request.ReserveFlashDealRequest request) { return body(HttpStatus.OK, service.reserve(request)); }
    @PostMapping("/confirm") public ApiResponse<Void> confirm(@Valid @RequestBody com.example.promotionservice.dto.request.FlashDealOrderRequest request) { service.confirm(request.orderId()); return body(HttpStatus.OK, null); }
    @PostMapping("/release") public ApiResponse<Void> release(@Valid @RequestBody com.example.promotionservice.dto.request.FlashDealOrderRequest request) { service.release(request.orderId()); return body(HttpStatus.OK, null); }
    private <T> ApiResponse<T> body(HttpStatus status, T data) { return ApiResponse.<T>builder().status(status.value()).message("Flash deals retrieved successfully").data(data).build(); }
}
