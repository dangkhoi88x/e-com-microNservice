package com.example.inventoryservice.exception;

import lombok.Getter;

@Getter
public class InventoryServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public InventoryServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
