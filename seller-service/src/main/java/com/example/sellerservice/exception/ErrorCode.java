package com.example.sellerservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SELLER_SHOP_NOT_FOUND(404, "Seller shop was not found", HttpStatus.NOT_FOUND),
    SELLER_SHOP_ALREADY_EXISTS(409, "This account already owns a seller shop", HttpStatus.CONFLICT),
    INVALID_SELLER_TRANSITION(400, "Seller shop status transition is invalid", HttpStatus.BAD_REQUEST),
    REVIEW_NOTE_REQUIRED(400, "A review note is required for this action", HttpStatus.BAD_REQUEST),
    IDENTITY_ROLE_GRANT_REJECTED(502, "Identity service rejected the seller role grant", HttpStatus.BAD_GATEWAY),
    IDENTITY_SERVICE_UNAVAILABLE(503, "Identity service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    SELLER_DATA_CONFLICT(409, "Seller shop data conflicts with an existing record", HttpStatus.CONFLICT),
    INVALID_REQUEST(400, "Request validation failed", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(500, "Unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
