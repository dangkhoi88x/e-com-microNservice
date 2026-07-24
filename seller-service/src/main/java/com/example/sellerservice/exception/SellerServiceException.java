package com.example.sellerservice.exception;

import lombok.Getter;

@Getter
public class SellerServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public SellerServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
