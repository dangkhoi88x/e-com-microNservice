package com.example.promotionservice.exception;

import lombok.Getter;

@Getter
public class PromotionServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public PromotionServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
