package com.example.inventoryservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetInventoryQuantityRequest(
        @NotNull @Min(0) Integer availableQuantity
) { }
