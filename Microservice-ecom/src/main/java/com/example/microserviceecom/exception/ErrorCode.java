package com.example.microserviceecom.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    USER_EXISTED(409, "user.existed", HttpStatus.CONFLICT),
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    MISSING_REFRESH_TOKEN(401, "Missing refresh token", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_ORIGIN(403, "Invalid refresh request origin", HttpStatus.FORBIDDEN),
    PASSWORD_RESET_CODE_INVALID(400, "Mã xác nhận không đúng hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_PASSWORD_MISMATCH(400, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_TOO_MANY_ATTEMPTS(429, "Bạn đã thử quá nhiều lần. Vui lòng yêu cầu mã mới", HttpStatus.TOO_MANY_REQUESTS),
    PASSWORD_RESET_REQUEST_LIMITED(429, "Vui lòng chờ trước khi yêu cầu mã mới", HttpStatus.TOO_MANY_REQUESTS),
    ;

    private final int code;
    private final String message;
    private final HttpStatus status;
}
