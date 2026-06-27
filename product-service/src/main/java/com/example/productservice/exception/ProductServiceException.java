package com.example.productservice.exception;

import lombok.Getter;

@Getter
public class ProductServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public ProductServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
