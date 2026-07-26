package com.example.mediaservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MediaServiceException.class)
    ResponseEntity<ErrorResponse> handleMedia(MediaServiceException exception, HttpServletRequest request) {
        return response(exception.getErrorCode(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MaxUploadSizeExceededException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> handleInvalidRequest(HttpServletRequest request) {
        return response(ErrorCode.INVALID_MEDIA_FILE, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(HttpServletRequest request) {
        return response(ErrorCode.INTERNAL_ERROR, request);
    }

    private ResponseEntity<ErrorResponse> response(ErrorCode errorCode, HttpServletRequest request) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build());
    }
}
