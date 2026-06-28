package com.example.productservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_ERROR(500, "Unexpected error occurred while processing request in backend service", HttpStatus.INTERNAL_SERVER_ERROR),

    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),

    CATEGORY_EXISTED(400, "Category already existed", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(404, "Category not found", HttpStatus.NOT_FOUND),
    INVALID_CATEGORY_NAME(400, "Category name is invalid", HttpStatus.BAD_REQUEST),
    CATEGORY_HAS_PRODUCTS(400, "Category still has products", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(404, "Product not found", HttpStatus.NOT_FOUND),
    INVALID_PRODUCT_NAME(400, "Product name is invalid", HttpStatus.BAD_REQUEST),
    PRODUCT_ACCESS_DENIED(403, "You do not have permission to access this product", HttpStatus.FORBIDDEN),

    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
