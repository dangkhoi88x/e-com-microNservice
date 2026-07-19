package com.example.wishlistservice.exception;

import lombok.Getter;
@Getter
public class WishlistServiceException extends RuntimeException {
    private final ErrorCode errorCode;
    public WishlistServiceException(ErrorCode errorCode) { super(errorCode.getMessage()); this.errorCode = errorCode; }
}
