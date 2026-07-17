package com.example.cartservice.exception;

import lombok.Getter;

@Getter
public class CartServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public CartServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
