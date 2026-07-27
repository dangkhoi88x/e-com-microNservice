package com.example.reviewservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    REVIEW_NOT_FOUND(404, "Review not found", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS(409, "This order item has already been reviewed", HttpStatus.CONFLICT),
    REVIEW_NOT_ELIGIBLE(400, "Only completed order items can be reviewed", HttpStatus.BAD_REQUEST),
    REVIEW_ACCESS_DENIED(403, "You are not allowed to change this review", HttpStatus.FORBIDDEN),
    INVALID_REVIEW_STATUS(400, "Review moderation status is invalid", HttpStatus.BAD_REQUEST),
    ORDER_SERVICE_UNAVAILABLE(503, "Order Service is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    REVIEW_DATA_CONFLICT(409, "Review data conflicts with an existing record", HttpStatus.CONFLICT),
    INVALID_REQUEST(400, "Request validation failed", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(500, "Unexpected error occurred while processing review", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
