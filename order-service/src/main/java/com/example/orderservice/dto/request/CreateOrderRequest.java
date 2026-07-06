package com.example.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "Shipping address cannot be blank")
        String shippingAddress,

        @NotEmpty(message = "Order items cannot be empty")
        List<@Valid OrderItemRequest> items

) {
}
