package com.example.inventoryservice.controller;

import com.example.inventoryservice.dto.request.InventoryOrderRequest;
import com.example.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trusted service-to-service endpoints used by the order payment workflow.
 * Public inventory mutations remain protected under /api/v1/inventory.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/inventory")
public class InternalInventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody InventoryOrderRequest request) {
        inventoryService.confirmInventory(request);
    }

    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@Valid @RequestBody InventoryOrderRequest request) {
        inventoryService.releaseInventory(request);
    }

    @PostMapping("/returns/{orderId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReturn(@PathVariable String orderId) {
        inventoryService.confirmReturnedInventory(orderId);
    }
}
