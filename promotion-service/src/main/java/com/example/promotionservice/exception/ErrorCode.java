package com.example.promotionservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    PROMOTION_NOT_FOUND(404, "Promotion campaign not found", HttpStatus.NOT_FOUND),
    PROMOTION_CODE_EXISTS(409, "Promotion code already exists", HttpStatus.CONFLICT),
    PROMOTION_NOT_ACTIVE(400, "Promotion campaign is not active", HttpStatus.BAD_REQUEST),
    PROMOTION_EXPIRED(400, "Promotion campaign has expired", HttpStatus.BAD_REQUEST),
    PROMOTION_MIN_ORDER_NOT_MET(400, "Order subtotal does not meet the promotion minimum", HttpStatus.BAD_REQUEST),
    INVALID_PROMOTION_PERIOD(400, "endAt must be after startAt", HttpStatus.BAD_REQUEST),
    PROMOTION_USAGE_LIMIT_REACHED(400, "Promotion usage limit has been reached", HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_CLAIMED(400, "Please claim this promotion before checkout", HttpStatus.BAD_REQUEST),
    FLASH_SALE_SOLD_OUT(409, "Flash Sale quota is no longer available", HttpStatus.CONFLICT),
    SALE_CAMPAIGN_OVERLAP(409, "This product or variant already has an overlapping campaign of the same sale type", HttpStatus.CONFLICT),
    FLASH_SALE_NOTIFICATION_UNAVAILABLE(400, "Flash Sale is not available for notification", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(400, "Request validation failed", HttpStatus.BAD_REQUEST),
    SELLER_PRODUCT_NOT_OWNED(403, "A sale campaign can only contain products owned by your shop", HttpStatus.FORBIDDEN),
    PRODUCT_SERVICE_UNAVAILABLE(503, "Product Service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(500, "Unexpected error occurred while processing promotion", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
