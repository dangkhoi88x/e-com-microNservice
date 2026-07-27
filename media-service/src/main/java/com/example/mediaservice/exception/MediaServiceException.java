package com.example.mediaservice.exception;

import lombok.Getter;

@Getter
public class MediaServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public MediaServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
