package com.example.inventoryservice.service;

import com.example.inventoryservice.dto.request.CreateInventoryRequest;
import com.example.inventoryservice.dto.request.InventoryOrderRequest;
import com.example.inventoryservice.dto.request.ReserveInventoryRequest;
import com.example.inventoryservice.dto.response.InventoryResponse;
import com.example.inventoryservice.dto.response.ReservationResponse;

import java.util.List;

public interface InventoryService {
    InventoryResponse createInventory(CreateInventoryRequest request);

    InventoryResponse getInventoryByProductId(String productId);

    void reserveInventory(ReserveInventoryRequest request);

    void confirmInventory(InventoryOrderRequest request);

    void releaseInventory(InventoryOrderRequest request);

    List<ReservationResponse> getReservationsByOrderId(String orderId);
}
