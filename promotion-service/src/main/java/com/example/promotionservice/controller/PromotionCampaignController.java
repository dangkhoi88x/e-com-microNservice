package com.example.promotionservice.controller;

import com.example.promotionservice.dto.request.CreatePromotionCampaignRequest;
import com.example.promotionservice.dto.request.ValidatePromotionRequest;
import com.example.promotionservice.dto.request.UpdatePromotionCampaignRequest;
import com.example.promotionservice.dto.response.ApiResponse;
import com.example.promotionservice.dto.response.PromotionCalculationResponse;
import com.example.promotionservice.dto.response.PromotionCampaignResponse;
import com.example.promotionservice.service.PromotionCampaignService;
import com.example.promotionservice.service.PromotionUsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/promotions/campaigns")
public class PromotionCampaignController {
    private final PromotionCampaignService service;
    private final PromotionUsageService usageService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<PromotionCampaignResponse> create(@Valid @RequestBody CreatePromotionCampaignRequest request) {
        return body(HttpStatus.CREATED, "Promotion campaign created successfully", service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<List<PromotionCampaignResponse>> getAll(@RequestParam(required = false) String status) {
        return body(HttpStatus.OK, "Promotion campaigns retrieved successfully", service.getAll(status));
    }

    @GetMapping("/active")
    public ApiResponse<List<PromotionCampaignResponse>> getActive() {
        return body(HttpStatus.OK, "Active promotions retrieved successfully", service.getAll("ACTIVE"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<PromotionCampaignResponse> getById(@PathVariable String id) {
        return body(HttpStatus.OK, "Promotion campaign retrieved successfully", service.getById(id));
    }

    @PostMapping("/preview")
    public ApiResponse<PromotionCalculationResponse> preview(
            @Valid @RequestBody ValidatePromotionRequest request) {
        return body(HttpStatus.OK, "Promotion preview calculated successfully", usageService.validate(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<PromotionCampaignResponse> update(@PathVariable String id, @Valid @RequestBody UpdatePromotionCampaignRequest request) {
        return body(HttpStatus.OK, "Promotion campaign updated successfully", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return body(HttpStatus.OK, "Promotion campaign deleted successfully", null);
    }

    private <T> ApiResponse<T> body(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).data(data).build();
    }
}
