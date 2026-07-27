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
    MULTI_SHOP_CHECKOUT_NOT_SUPPORTED(400, "For this demo, please checkout products from one shop at a time", HttpStatus.BAD_REQUEST),
    SELLER_ORDER_TRANSITION_INVALID(400, "Seller cannot perform this order transition", HttpStatus.BAD_REQUEST),
    INVENTORY_RESERVATION_FAILED(409, "Inventory reservation failed", HttpStatus.CONFLICT),
    ORDER_CANNOT_BE_CANCELLED(400, "Order cannot be cancelled", HttpStatus.BAD_REQUEST),
    CART_CHECKOUT_EMPTY(400, "No cart items are available for checkout. You may already have an order pending payment; complete or cancel it in My Orders before checking out again", HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_APPLICABLE(400, "Promotion is invalid or not applicable", HttpStatus.BAD_REQUEST),
    PROMOTION_SERVICE_UNAVAILABLE(503, "Promotion Service Unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    PROMOTION_RESERVATION_FAILED(409, "Promotion reservation failed", HttpStatus.CONFLICT),
    FLASH_SALE_RESERVATION_FAILED(409, "Flash Sale is no longer available", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;


}
