package com.example.profileservice.exception;

import lombok.Getter;

@Getter
public class ProfileServiceException extends RuntimeException {

    private final ErrorCode errorCode;

    public ProfileServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
