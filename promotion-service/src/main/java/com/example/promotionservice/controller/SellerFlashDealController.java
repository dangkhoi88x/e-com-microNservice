package com.example.promotionservice.controller;

import com.example.promotionservice.dto.request.CreateFlashDealRequest;
import com.example.promotionservice.dto.response.ApiResponse;
import com.example.promotionservice.dto.response.FlashDealDetailResponse;
import com.example.promotionservice.dto.response.FlashDealResponse;
import com.example.promotionservice.service.FlashDealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/flash-deals/seller")
@PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'SELLER')")
public class SellerFlashDealController {
    private final FlashDealService service;

    @PostMapping
    public ApiResponse<FlashDealResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateFlashDealRequest request) {
        return body(HttpStatus.CREATED, service.createForSeller(jwt.getSubject(), request));
    }

    @GetMapping
    public ApiResponse<List<FlashDealResponse>> getMine(@AuthenticationPrincipal Jwt jwt) {
        return body(HttpStatus.OK, service.getAllForSeller(jwt.getSubject()));
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<FlashDealDetailResponse> detail(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return body(HttpStatus.OK, service.getDetailForSeller(id, jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public ApiResponse<FlashDealResponse> update(@PathVariable String id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateFlashDealRequest request) {
        return body(HttpStatus.OK, service.updateForSeller(id, jwt.getSubject(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        service.deleteForSeller(id, jwt.getSubject());
        return body(HttpStatus.OK, null);
    }

    private <T> ApiResponse<T> body(HttpStatus status, T data) {
        return ApiResponse.<T>builder().status(status.value()).message("Seller flash deal processed successfully").data(data).build();
    }
}
