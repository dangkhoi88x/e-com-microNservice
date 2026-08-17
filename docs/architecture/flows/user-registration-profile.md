# Flow — Tạo user, profile và welcome notification

## Scope

Flow này bắt đầu tại `POST /users`. Identity Service lưu user, phát `UserCreatedEvent` sau commit. Profile Service tạo profile rồi phát event tiếp theo để Notification Service lưu welcome notification và gửi email. Khi Profile Service hết retry, nó phát event compensation để Identity Service xoá user đã tạo.

![Sequence tạo user, profile và notification](../diagrams/user-registration-profile-sequence.png)

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant GW as API Gateway
    participant Identity as Identity Service
    participant UserDB as Identity database
    participant Kafka as Kafka
    participant Profile as Profile Service
    participant ProfileDB as Profile database
    participant Notify as Notification Service
    participant NotifyDB as Notification database
    participant SMTP as SMTP

    User->>GW: POST /identity/users
    GW->>Identity: Forward REST/HTTPS
    Identity->>UserDB: Lưu User (JPA)
    UserDB-->>Identity: Commit
    Identity->>Kafka: Publish created-user-topic
    Kafka-->>Profile: Deliver UserCreatedEvent
    Profile->>ProfileDB: Tạo UserProfile
    Profile->>Kafka: Publish created-profile-created
    Kafka-->>Notify: Deliver UserProfileCreatedEvent
    Notify->>NotifyDB: Lưu welcome notification
    Notify->>SMTP: Gửi welcome email
    alt Profile tạo thất bại sau retry
        Profile->>Kafka: Publish user-profile-created-fail
        Kafka-->>Identity: Deliver compensation event
        Identity->>UserDB: Xoá User
    end
```

## Activity: provisioning và compensation

![Activity tạo user, profile và compensation](../diagrams/user-registration-profile-activity.png)

```mermaid
flowchart TD
    start([Tạo user]) --> saveUser["Lưu User trong Identity Service"]
    saveUser --> committed{"Commit thành công?"}
    committed -->|Không| failed["Trả lỗi tạo user"]
    committed -->|Có| publishUser["Publish created-user-topic"]
    publishUser --> createProfile["Profile Service tạo UserProfile"]
    createProfile --> profileOk{"Tạo profile thành công?"}
    profileOk -->|Có| publishProfile["Publish created-profile-created"]
    publishProfile --> welcome["Lưu notification và gửi email"]
    profileOk -->|Không, hết retry| compensate["Publish user-profile-created-fail"]
    compensate --> deleteUser["Identity Service xoá User"]
    welcome --> done([Hoàn tất])
    failed --> done
    deleteUser --> done
```

## Evidence

- API và tạo user: `Microservice-ecom/src/main/java/com/example/microserviceecom/controller/UserController.java`, `service/UserService.java`.
- Publish sau commit: `messaging/producer/UserCreatedEventKafkaPublisher.java`.
- Tạo profile, retry/DLT và compensation: `profile-service/.../messaing/consumer/UserProfileConsumer.java`.
- Welcome notification/email: `notification-service/.../messaging/consumer/UserCreatedConsumer.java`.

## Chưa xác minh

- Độ bền của event publish giữa transaction DB và Kafka chưa có outbox pattern được xác minh trong code đã đọc.
- Nội dung và cấu hình SMTP thực tế phụ thuộc runtime configuration; không được đưa vào tài liệu.
