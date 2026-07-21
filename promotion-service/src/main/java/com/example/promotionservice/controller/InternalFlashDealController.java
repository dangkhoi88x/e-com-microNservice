package com.example.promotionservice.controller;

import com.example.promotionservice.dto.request.*;
import com.example.promotionservice.dto.response.*;
import com.example.promotionservice.service.FlashDealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/flash-deals")
public class InternalFlashDealController {
    private final FlashDealService service;
    @PostMapping("/reserve") public ApiResponse<List<FlashDealPriceResponse>> reserve(@Valid @RequestBody ReserveFlashDealRequest request) { return body(HttpStatus.OK, service.reserve(request)); }
    @PostMapping("/confirm") public ApiResponse<Void> confirm(@Valid @RequestBody FlashDealOrderRequest request) { service.confirm(request.orderId()); return body(HttpStatus.OK, null); }
    @PostMapping("/release") public ApiResponse<Void> release(@Valid @RequestBody FlashDealOrderRequest request) { service.release(request.orderId()); return body(HttpStatus.OK, null); }
    private <T> ApiResponse<T> body(HttpStatus status, T data) { return ApiResponse.<T>builder().status(status.value()).message("Flash deal operation completed").data(data).build(); }
}
