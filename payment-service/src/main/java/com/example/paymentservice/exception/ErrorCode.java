package com.example.paymentservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_ERROR(500, "Unexpected error occurred while processing request in backend service", HttpStatus.INTERNAL_SERVER_ERROR),

    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),

    ORDER_NOT_FOUND(404, "Order not found", HttpStatus.NOT_FOUND),
    ORDER_ACCESS_DENIED(403, "You do not have permission to access this order", HttpStatus.FORBIDDEN),
    ORDER_SERVICE_UNAVAILABLE(503, "Order service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    ORDER_NOT_PAYABLE(400, "Order is not payable", HttpStatus.BAD_REQUEST),

    PAYMENT_NOT_FOUND(404, "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_ACCESS_DENIED(403, "You do not have permission to access this payment", HttpStatus.FORBIDDEN),
    PAYMENT_ALREADY_EXISTS(409, "A pending payment already exists for this order", HttpStatus.CONFLICT),
    PAYMENT_ALREADY_SUCCESS(400, "Payment has already been completed", HttpStatus.BAD_REQUEST),
    PAYMENT_CANNOT_BE_COMPLETED(400, "Payment cannot be completed", HttpStatus.BAD_REQUEST),
    PAYMENT_CANNOT_BE_FAILED(400, "Payment cannot be failed", HttpStatus.BAD_REQUEST),
    PAYMENT_CANNOT_BE_CANCELLED(400, "Payment cannot be cancelled", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_STATUS(400, "Invalid payment status", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_METHOD(400, "Invalid payment method", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_STRIPE(400, "This payment does not use Stripe", HttpStatus.BAD_REQUEST),
    STRIPE_REQUIRES_WEBHOOK(400, "Stripe payments can only be completed by a verified webhook", HttpStatus.BAD_REQUEST),

    STRIPE_NOT_CONFIGURED(503, "Stripe is not configured", HttpStatus.SERVICE_UNAVAILABLE),
    STRIPE_CHECKOUT_FAILED(502, "Could not create Stripe checkout session", HttpStatus.BAD_GATEWAY),
    INVALID_STRIPE_WEBHOOK(400, "Invalid Stripe webhook", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;


}
