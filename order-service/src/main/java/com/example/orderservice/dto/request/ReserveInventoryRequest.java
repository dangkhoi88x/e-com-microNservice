package com.example.orderservice.dto.request;

import java.util.List;

public record ReserveInventoryRequest(
        String orderId,
        List<ReserveInventoryItemRequest> items
) {
}