package com.example.sellerservice.controller;

import com.example.sellerservice.dto.request.ReviewSellerShopRequest;
import com.example.sellerservice.dto.response.ApiResponse;
import com.example.sellerservice.dto.response.SellerShopResponse;
import com.example.sellerservice.entity.SellerStatus;
import com.example.sellerservice.service.SellerShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sellers/admin")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
public class AdminSellerShopController {
    private final SellerShopService sellerShopService;

    @GetMapping
    public ApiResponse<Page<SellerShopResponse>> getAll(
            @RequestParam(required = false) SellerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return response(HttpStatus.OK, "Seller shops retrieved successfully",
                sellerShopService.getAll(status, pageable));
    }

    @PutMapping("/{shopId}/review")
    public ApiResponse<SellerShopResponse> review(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody ReviewSellerShopRequest request
    ) {
        return response(HttpStatus.OK, "Seller shop review completed",
                sellerShopService.reviewShop(shopId, jwt.getSubject(), authorization, request));
    }

    private <T> ApiResponse<T> response(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder().status(status.value()).message(message).data(data).build();
    }
}
