package com.example.reviewservice.exception;

import lombok.Getter;

@Getter
public class ReviewServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public ReviewServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
