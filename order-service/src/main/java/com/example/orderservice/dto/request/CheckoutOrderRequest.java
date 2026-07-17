package com.example.orderservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CheckoutOrderRequest(@NotBlank(message = "Shipping address cannot be blank") String shippingAddress) {}
