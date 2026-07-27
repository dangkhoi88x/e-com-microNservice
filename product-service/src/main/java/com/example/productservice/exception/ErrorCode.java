package com.example.productservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_ERROR(500, "Unexpected error occurred while processing request in backend service", HttpStatus.INTERNAL_SERVER_ERROR),

    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),

    CATEGORY_EXISTED(400, "Category already existed", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(404, "Category not found", HttpStatus.NOT_FOUND),
    INVALID_CATEGORY_NAME(400, "Category name is invalid", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_PRODUCTS(400, "Category still has products", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(404, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_OUT_OF_STOCK(400, "Product out of stock", HttpStatus.BAD_REQUEST),
    INVALID_PRODUCT_NAME(400, "Product name is invalid", HttpStatus.BAD_REQUEST),
    DUPLICATE_PRODUCT_VARIANT_SKU(400, "Product variant SKU already exists", HttpStatus.BAD_REQUEST),
    INVALID_PRODUCT_VARIANT_ATTRIBUTE(400, "Product variant attributes must match the configured product options", HttpStatus.BAD_REQUEST),
    INVALID_CATEGORY_ID(400, "Category id is invalid", HttpStatus.BAD_REQUEST),
    PRODUCT_ACCESS_DENIED(403, "You do not have permission to access this product", HttpStatus.FORBIDDEN),
    SELLER_SHOP_NOT_APPROVED(403, "Your shop must be approved before managing products", HttpStatus.FORBIDDEN),
    SELLER_SERVICE_UNAVAILABLE(503, "Seller Service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_PRODUCT_TRANSITION(400, "Product status transition is invalid", HttpStatus.BAD_REQUEST),
    MODERATION_NOTE_REQUIRED(400, "A moderation note is required for this action", HttpStatus.BAD_REQUEST),

    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
