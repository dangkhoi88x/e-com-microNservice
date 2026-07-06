package com.example.searchservice.exception;

import lombok.Getter;

@Getter
public class SearchServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public SearchServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
