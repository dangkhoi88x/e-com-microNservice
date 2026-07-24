package com.example.sellerservice.service;

import com.example.sellerservice.dto.request.CreateSellerShopRequest;
import com.example.sellerservice.dto.request.ReviewSellerShopRequest;
import com.example.sellerservice.dto.request.UpdateSellerShopRequest;
import com.example.sellerservice.dto.response.SellerEligibilityResponse;
import com.example.sellerservice.dto.response.SellerShopResponse;
import com.example.sellerservice.entity.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SellerShopService {
    SellerShopResponse createMyShop(String ownerUserId, CreateSellerShopRequest request);

    SellerShopResponse getMyShop(String ownerUserId);

    SellerShopResponse updateMyShop(String ownerUserId, UpdateSellerShopRequest request);

    SellerShopResponse resubmitMyShop(String ownerUserId);

    SellerEligibilityResponse getMyEligibility(String ownerUserId);

    Page<SellerShopResponse> getAll(SellerStatus status, Pageable pageable);

    SellerShopResponse getById(UUID shopId);

    SellerShopResponse reviewShop(UUID shopId, String adminUserId, String authorization, ReviewSellerShopRequest request);
}
