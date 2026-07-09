package com.example.inventoryservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),
    INVENTORY_NOT_FOUND(404, "Inventory not found", HttpStatus.NOT_FOUND),
    INVENTORY_ALREADY_EXISTS(409, "Inventory already exists for this product", HttpStatus.CONFLICT),
    INVENTORY_ALREADY_RESERVED(409, "Inventory already reserved for this order", HttpStatus.CONFLICT),
    RESERVATION_NOT_FOUND(404, "Inventory reservation not found", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(400, "Insufficient stock", HttpStatus.BAD_REQUEST),
    INVALID_INVENTORY_REQUEST(400, "Invalid inventory request", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(500, "Unexpected error occurred while processing request in inventory service", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
