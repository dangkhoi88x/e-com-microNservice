package com.example.shippingservice.exception;

import lombok.Getter;

@Getter
public class ShippingServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public ShippingServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
