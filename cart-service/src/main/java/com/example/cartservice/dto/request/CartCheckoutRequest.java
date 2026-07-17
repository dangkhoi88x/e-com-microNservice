package com.example.cartservice.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record CartCheckoutRequest(@NotBlank String orderId, @NotEmpty List<String> itemIds) {}
