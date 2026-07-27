package com.example.promotionservice.service;

import com.example.promotionservice.dto.request.CreateFlashDealRequest;
import com.example.promotionservice.dto.response.FlashDealResponse;
import com.example.promotionservice.dto.response.FlashDealDetailResponse;
import com.example.promotionservice.dto.request.ReserveFlashDealRequest;
import com.example.promotionservice.dto.response.FlashDealPriceResponse;
import com.example.promotionservice.entity.FlashDealStatus;
import com.example.promotionservice.entity.SaleType;
import java.util.List;

public interface FlashDealService {
    FlashDealResponse create(CreateFlashDealRequest request);
    FlashDealResponse createForSeller(String sellerId, CreateFlashDealRequest request);
    List<FlashDealResponse> getAll(String status);
    List<FlashDealResponse> getAllForSeller(String sellerId);
    List<FlashDealResponse> getByStatusAndType(FlashDealStatus status, SaleType saleType);
    FlashDealResponse getById(String id);
    FlashDealDetailResponse getDetail(String id);
    FlashDealResponse update(String id, CreateFlashDealRequest request);
    FlashDealResponse updateForSeller(String id, String sellerId, CreateFlashDealRequest request);
    void delete(String id);
    void deleteForSeller(String id, String sellerId);
    FlashDealDetailResponse getDetailForSeller(String id, String sellerId);
    List<FlashDealPriceResponse> reserve(ReserveFlashDealRequest request);
    void confirm(String orderId);
    void release(String orderId);
    void subscribeForNotification(String flashDealId, String userId);
    List<String> getNotificationSubscriptions(String userId);
}
