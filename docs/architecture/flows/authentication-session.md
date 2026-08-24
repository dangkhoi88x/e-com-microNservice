# Flow — Login, refresh token và logout

## Scope

Identity Service xác thực email/password, tạo JWT access token và refresh token. Controller chỉ trả access token trong response; refresh token nằm trong cookie `HttpOnly`. Refresh endpoint kiểm tra origin được phép, tra session Redis, tạo access token mới và giữ refresh token hiện tại. Logout đưa access-token ID vào cơ chế revoke và xoá refresh session.

![Sequence authentication và session](../diagrams/authentication-session-sequence.png)

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant UI as web-app
    participant GW as API Gateway
    participant Identity as Identity Service
    participant UserDB as Identity database
    participant Redis as Redis

    User->>UI: Nhập email và password
    UI->>GW: POST /identity/auth/login
    GW->>Identity: Forward REST/HTTPS
    Identity->>UserDB: Đọc user và password hash
    Identity->>Identity: AuthenticationManager xác thực
    Identity->>Identity: Tạo JWT access token
    Identity->>Redis: Lưu refresh session 14 ngày
    Identity-->>UI: 200 accessToken + Set-Cookie refresh_token HttpOnly
    opt Access token hết hạn
        UI->>GW: POST /identity/auth/refresh-token + cookie
        GW->>Identity: Forward REST/HTTPS + Origin
        Identity->>Identity: Kiểm tra allowed origin
        Identity->>Redis: Tìm refresh session
        Identity->>UserDB: Đọc user/roles/authVersion
        Identity-->>UI: 200 accessToken mới + cookie
    end
    User->>UI: Logout
    UI->>GW: POST /identity/auth/logout
    GW->>Identity: Forward REST/HTTPS
    Identity->>Redis: Revoke access token ID và xoá refresh session
    Identity-->>UI: 200 + clear refresh cookie
```

## Activity: session lifecycle

![Activity authentication và session](../diagrams/authentication-session-activity.png)

```mermaid
flowchart TD
    start([Login request]) --> credentials{"Email/password hợp lệ?"}
    credentials -->|Không| unauthorized["Trả UNAUTHORIZED"]
    credentials -->|Có| access["Tạo JWT access token"]
    access --> refresh["Lưu refresh session Redis"]
    refresh --> cookie["Trả access token và cookie HttpOnly"]
    cookie --> expired{"Access token hết hạn?"}
    expired -->|Không| active["Gọi protected API"]
    expired -->|Có| origin{"Origin và refresh session hợp lệ?"}
    origin -->|Có| renew["Tạo access token mới"]
    origin -->|Không| refreshError["Trả lỗi refresh"]
    renew --> active
    active --> logout["Logout: revoke access token và xoá session"]
    logout --> done([Hoàn tất])
    unauthorized --> done
    refreshError --> done
```

## Evidence

- Auth endpoints/cookie/origin check: `Microservice-ecom/.../controller/AuthenticationController.java`.
- Login, refresh và logout: `Microservice-ecom/.../service/AuthenticationService.java`.
- Password reset invalidates sessions: `Microservice-ecom/.../service/PasswordResetService.java`.

## Chưa xác minh

- JWT expiry, Redis retention và allowed origin values là runtime configuration; không ghi giá trị vào tài liệu.
