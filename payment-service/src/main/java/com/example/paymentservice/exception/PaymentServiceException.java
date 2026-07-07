package com.example.paymentservice.exception;

import lombok.Getter;

@Getter
public class PaymentServiceException extends RuntimeException{
    private final ErrorCode errorCode;

    public PaymentServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
