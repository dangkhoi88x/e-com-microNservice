package com.example.mediaservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_MEDIA_FILE(400, "Only JPEG, PNG, and WebP images up to the configured size are allowed", HttpStatus.BAD_REQUEST),
    MEDIA_NOT_FOUND(404, "Media asset was not found", HttpStatus.NOT_FOUND),
    MEDIA_ACCESS_DENIED(403, "You do not have permission to access this media asset", HttpStatus.FORBIDDEN),
    S3_NOT_CONFIGURED(503, "Media storage is not configured", HttpStatus.SERVICE_UNAVAILABLE),
    MEDIA_STORAGE_UNAVAILABLE(503, "Media storage is unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR(500, "Unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
