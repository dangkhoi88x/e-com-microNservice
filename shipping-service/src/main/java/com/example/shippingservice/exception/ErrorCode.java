package com.example.shippingservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SHIPMENT_NOT_FOUND(404, "Shipment not found", HttpStatus.NOT_FOUND),
    SHIPMENT_ALREADY_EXISTS(409, "Shipment already exists for this order", HttpStatus.CONFLICT),
    TRACKING_NUMBER_EXISTS(409, "Tracking number already exists", HttpStatus.CONFLICT),
    INVALID_SHIPMENT_TRANSITION(409, "Shipment status transition is not allowed", HttpStatus.CONFLICT),
    SHIPMENT_ACCESS_DENIED(403, "You are not allowed to access this shipment", HttpStatus.FORBIDDEN),
    ORDER_NOT_CONFIRMED(400, "Order must be confirmed before creating a shipment", HttpStatus.BAD_REQUEST),
    SHIPMENT_ALREADY_DELIVERED(409, "Delivered shipment cannot be changed", HttpStatus.CONFLICT),
    CONCURRENT_SHIPMENT_UPDATE(409, "Shipment was updated by another request", HttpStatus.CONFLICT),
    SHIPMENT_DATA_CONFLICT(409, "Shipment data conflicts with an existing record", HttpStatus.CONFLICT),
    INVALID_REQUEST(400, "Request validation failed", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(500, "Unexpected error occurred while processing shipment", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
