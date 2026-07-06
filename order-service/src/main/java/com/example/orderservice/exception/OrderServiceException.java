package com.example.orderservice.exception;

import lombok.Getter;

@Getter
public class OrderServiceException extends RuntimeException{
    private final ErrorCode errorCode;

    public OrderServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
