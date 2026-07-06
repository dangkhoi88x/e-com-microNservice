package com.example.orderservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    PRODUCT_NOT_FOUND(404, "Product Not Found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_ACTIVE(400, "Product is not active", HttpStatus.BAD_REQUEST),
    PRODUCT_OUT_OF_STOCK(400, "Product out of stock", HttpStatus.BAD_REQUEST),
    PRODUCT_SERVICE_UNAVAILABLE(503, "Product Service Unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    ORDER_NOT_FOUND(404, "Order Not Found", HttpStatus.NOT_FOUND),
    ORDER_ACCESS_DENIED(403, "Order Access Denied", HttpStatus.FORBIDDEN),
    ORDER_CANNOT_BE_CANCELLED(400, "Order cannot be cancelled", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;


}
