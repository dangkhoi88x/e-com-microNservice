package com.example.cartservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_ERROR(500, "Unexpected error occurred while processing cart", HttpStatus.INTERNAL_SERVER_ERROR),
    CART_ITEM_NOT_FOUND(404, "Cart item not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(404, "Product not found", HttpStatus.NOT_FOUND),
    PRODUCT_VARIANT_NOT_FOUND(404, "Product variant not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_ACTIVE(400, "Product is not active", HttpStatus.BAD_REQUEST),
    INVALID_CART_ITEM_ID(400, "Cart item id is invalid", HttpStatus.BAD_REQUEST),
    FORBIDDEN(403, "You do not have permission to access this cart item", HttpStatus.FORBIDDEN),
    PRODUCT_SERVICE_UNAVAILABLE(503, "Product service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
