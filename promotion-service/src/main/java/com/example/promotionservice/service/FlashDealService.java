package com.example.promotionservice.service;

import com.example.promotionservice.dto.request.CreateFlashDealRequest;
import com.example.promotionservice.dto.response.FlashDealResponse;
import com.example.promotionservice.dto.request.ReserveFlashDealRequest;
import com.example.promotionservice.dto.response.FlashDealPriceResponse;
import com.example.promotionservice.entity.FlashDealStatus;
import com.example.promotionservice.entity.SaleType;
import java.util.List;

public interface FlashDealService {
    FlashDealResponse create(CreateFlashDealRequest request);
    List<FlashDealResponse> getAll(String status);
    List<FlashDealResponse> getByStatusAndType(FlashDealStatus status, SaleType saleType);
    FlashDealResponse getById(String id);
    FlashDealResponse update(String id, CreateFlashDealRequest request);
    void delete(String id);
    List<FlashDealPriceResponse> reserve(ReserveFlashDealRequest request);
    void confirm(String orderId);
    void release(String orderId);
    void subscribeForNotification(String flashDealId, String userId);
    List<String> getNotificationSubscriptions(String userId);
}
