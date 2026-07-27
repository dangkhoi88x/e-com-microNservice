package com.example.sellerservice.controller;

import com.example.sellerservice.dto.request.CreateSellerShopRequest;
import com.example.sellerservice.dto.request.UpdateSellerShopRequest;
import com.example.sellerservice.dto.response.ApiResponse;
import com.example.sellerservice.dto.response.SellerEligibilityResponse;
import com.example.sellerservice.dto.response.SellerShopResponse;
import com.example.sellerservice.service.SellerShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sellers/me")
public class SellerShopController {
    private final SellerShopService sellerShopService;

    @PostMapping("/shop")
    public ApiResponse<SellerShopResponse> createMyShop(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSellerShopRequest request
    ) {
        return response(HttpStatus.CREATED, "Seller shop submitted for approval",
                sellerShopService.createMyShop(jwt.getSubject(), request));
    }

    @GetMapping
    public ApiResponse<SellerShopResponse> getMyShop(@AuthenticationPrincipal Jwt jwt) {
        return response(HttpStatus.OK, "Seller shop retrieved successfully",
                sellerShopService.getMyShop(jwt.getSubject()));
    }

    @PutMapping("/shop")
    public ApiResponse<SellerShopResponse> updateMyShop(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateSellerShopRequest request
    ) {
        return response(HttpStatus.OK, "Seller shop updated successfully",
                sellerShopService.updateMyShop(jwt.getSubject(), request));
    }

    @PostMapping("/shop/resubmit")
    public ApiResponse<SellerShopResponse> resubmitMyShop(@AuthenticationPrincipal Jwt jwt) {
        return response(HttpStatus.OK, "Seller shop resubmitted for approval",
                sellerShopService.resubmitMyShop(jwt.getSubject()));
    }

    @GetMapping("/eligibility")
    public ApiResponse<SellerEligibilityResponse> getMyEligibility(@AuthenticationPrincipal Jwt jwt) {
        return response(HttpStatus.OK, "Seller eligibility retrieved successfully",
                sellerShopService.getMyEligibility(jwt.getSubject()));
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).data(data).build();
    }
}
