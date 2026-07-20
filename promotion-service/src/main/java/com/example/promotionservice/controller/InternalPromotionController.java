package com.example.promotionservice.controller;

import com.example.promotionservice.dto.request.PromotionOrderRequest;
import com.example.promotionservice.dto.request.ReservePromotionRequest;
import com.example.promotionservice.dto.request.ValidatePromotionRequest;
import com.example.promotionservice.dto.response.ApiResponse;
import com.example.promotionservice.dto.response.PromotionCalculationResponse;
import com.example.promotionservice.service.PromotionUsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/promotions")
public class InternalPromotionController {
    private final PromotionUsageService service;

    @PostMapping("/validate")
    public ApiResponse<PromotionCalculationResponse> validate(@Valid @RequestBody ValidatePromotionRequest request) {
        return response(HttpStatus.OK, "Promotion validated", service.validate(request));
    }

    @PostMapping("/reserve")
    public ApiResponse<PromotionCalculationResponse> reserve(@Valid @RequestBody ReservePromotionRequest request) {
        return response(HttpStatus.OK, "Promotion reserved", service.reserve(request));
    }

    @PostMapping("/confirm")
    public ApiResponse<Void> confirm(@Valid @RequestBody PromotionOrderRequest request) {
        service.confirm(request);
        return response(HttpStatus.OK, "Promotion confirmed", null);
    }

    @PostMapping("/release")
    public ApiResponse<Void> release(@Valid @RequestBody PromotionOrderRequest request) {
        service.release(request);
        return response(HttpStatus.OK, "Promotion released", null);
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).data(data).build();
    }
}
