package com.example.wishlistservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(WishlistServiceException.class)
    ResponseEntity<ErrorResponse> handleWishlist(WishlistServiceException exception, HttpServletRequest request) { return response(exception.getErrorCode(), request); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(HttpServletRequest request) { return response(ErrorCode.INTERNAL_ERROR, request); }
    private ResponseEntity<ErrorResponse> response(ErrorCode code, HttpServletRequest request) { return ResponseEntity.status(code.getHttpStatus()).body(ErrorResponse.builder().code(code.getCode()).message(code.getMessage()).path(request.getRequestURI()).timestamp(Instant.now()).build()); }
}
