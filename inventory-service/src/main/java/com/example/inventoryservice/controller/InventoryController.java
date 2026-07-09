package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.request.BatchInventoryRequest;
import com.example.inventoryservice.dto.request.CreateInventoryRequest;
import com.example.inventoryservice.dto.request.InventoryOrderRequest;
import com.example.inventoryservice.dto.request.ReserveInventoryRequest;
import com.example.inventoryservice.dto.response.InventoryResponse;
import com.example.inventoryservice.dto.response.ReservationResponse;
import com.example.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        return inventoryService.createInventory(request);
    }

    @GetMapping("/products/{productId}")
    public InventoryResponse getInventoryByProductId(@PathVariable String productId) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @PostMapping("/products/batch")
    public List<InventoryResponse> getInventoriesByProductIds(@Valid @RequestBody BatchInventoryRequest request) {
        return inventoryService.getInventoriesByProductIds(request.productIds());
    }

    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reserveInventory(@Valid @RequestBody ReserveInventoryRequest request) {
        inventoryService.reserveInventory(request);
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmInventory(@Valid @RequestBody InventoryOrderRequest request) {
        inventoryService.confirmInventory(request);
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseInventory(@Valid @RequestBody InventoryOrderRequest request) {
        inventoryService.releaseInventory(request);
    }

    @GetMapping("/reservations/orders/{orderId}")
    public List<ReservationResponse> getReservationsByOrderId(@PathVariable String orderId) {
        return inventoryService.getReservationsByOrderId(orderId);
    }
}
