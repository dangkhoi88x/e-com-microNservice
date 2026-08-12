package com.example.orderservice.exception;

import lombok.Getter;

@Getter
public class OrderServiceException extends RuntimeException{
    private final ErrorCode errorCode;

    public OrderServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** Keeps the downstream failure attached so logs show why a call actually failed. */
    public OrderServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
