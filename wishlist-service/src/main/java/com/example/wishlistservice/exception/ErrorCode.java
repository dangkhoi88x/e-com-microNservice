package com.example.wishlistservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter @RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_ERROR(500, "Unexpected error occurred while processing wishlist", HttpStatus.INTERNAL_SERVER_ERROR),
    PRODUCT_NOT_FOUND(404, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_VARIANT_NOT_FOUND(404, "Product variant not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_ACTIVE(400, "Product is not active", HttpStatus.BAD_REQUEST),
    PRODUCT_SERVICE_UNAVAILABLE(503, "Product service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
