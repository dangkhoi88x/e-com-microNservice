# Flow — Yêu cầu và xác nhận đặt lại mật khẩu

## Scope

Identity Service nhận request reset, chỉ gửi OTP khi email tồn tại. OTP được lưu Redis 10 phút; Notification Service kiểm tra header nội bộ bằng biến cấu hình rồi gửi email bất đồng bộ. Khi confirm, Identity Service kiểm tra OTP, đổi password, tăng `authVersion` và thu hồi refresh session.

![Sequence đặt lại mật khẩu](../diagrams/password-reset-sequence.png)

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant GW as API Gateway
    participant Identity as Identity Service
    participant UserDB as Identity database
    participant Redis as Redis
    participant Notify as Notification Service
    participant SMTP as SMTP

    User->>GW: POST /identity/auth/password-reset/request
    GW->>Identity: Forward REST/HTTPS
    Identity->>UserDB: Tìm user theo email
    alt Email tồn tại
        Identity->>Redis: Lưu OTP TTL 10 phút
        Identity->>Notify: POST /internal/password-reset-email (REST, X-Internal-Api-Key)
        Notify->>SMTP: Gửi email OTP bất đồng bộ
    end
    Identity-->>User: 200, thông điệp không tiết lộ email
    User->>GW: POST /identity/auth/password-reset/confirm
    GW->>Identity: Forward REST/HTTPS
    Identity->>Redis: Đọc và xoá OTP
    alt OTP hợp lệ và password khớp
        Identity->>UserDB: Lưu password hash, tăng authVersion
        Identity->>Redis: Thu hồi refresh sessions
        Identity-->>User: 200 reset thành công
    else OTP hoặc input không hợp lệ
        Identity-->>User: Domain error
    end
```

## Activity: kiểm tra OTP

![Activity kiểm tra OTP](../diagrams/password-reset-activity.png)

```mermaid
flowchart TD
    start([Yêu cầu reset]) --> findUser["Tìm user theo email"]
    findUser --> exists{"Email tồn tại?"}
    exists -->|Có| saveOtp["Sinh và lưu OTP Redis, TTL 10 phút"]
    saveOtp --> sendMail["Gọi Notification Service gửi OTP"]
    sendMail --> response["Trả phản hồi trung tính"]
    exists -->|Không| response
    response --> confirm["Nhận confirm email, OTP, password"]
    confirm --> match{"Password khớp và OTP hợp lệ?"}
    match -->|Không| invalid["Trả lỗi reset"]
    match -->|Có| updateUser["Xoá OTP, cập nhật password và authVersion"]
    updateUser --> revoke["Thu hồi refresh sessions"]
    revoke --> done([Hoàn tất])
    invalid --> done
```

## Evidence

- Public endpoints: `Microservice-ecom/.../controller/AuthenticationController.java`.
- OTP, Redis và session revocation: `Microservice-ecom/.../service/PasswordResetService.java`.
- REST internal mail client: `Microservice-ecom/.../client/PasswordResetMailClient.java`.
- Header validation và email: `notification-service/.../controller/InternalMailController.java`, `service/MailService.java`.

## Chưa xác minh

- Giá trị `services.notification.internal-api-key`, SMTP credential, và Redis endpoint là secret/runtime configuration nên không được hiển thị.
- Không suy ra rate limiting hoặc CAPTCHA cho endpoint reset từ code đã kiểm tra.
