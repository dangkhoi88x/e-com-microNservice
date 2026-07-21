package com.example.promotionservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ReserveFlashDealRequest(@NotBlank String orderId, @NotEmpty List<@Valid FlashDealOrderItemRequest> items) {}
