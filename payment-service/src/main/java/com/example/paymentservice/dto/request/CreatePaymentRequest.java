package com.example.paymentservice.dto.request;

import com.example.paymentservice.common.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(@NotBlank(message = "Order id cannot be blank")
                                    String orderId,

                                   @NotNull(message = "Payment method is required")
                                   PaymentMethod method) {
}
