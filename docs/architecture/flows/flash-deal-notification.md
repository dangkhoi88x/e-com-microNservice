# Flow — Đăng ký flash deal và thông báo sắp diễn ra

## Scope

User có thể subscribe một flash deal `SCHEDULED` hoặc subscribe thông báo chung. Scheduled job của Promotion Service định kỳ đồng bộ status, materialize subscription chung cho deal bắt đầu trong 15 phút và phát event `flash-sale-upcoming`. Notification Service consume event để lưu in-app notification.

![Sequence flash deal notification](../diagrams/flash-deal-notification-sequence.png)

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant GW as API Gateway
    participant Promo as Promotion Service
    participant PromoDB as Promotion database
    participant Scheduler as Promotion scheduler
    participant Kafka as Kafka
    participant Notify as Notification Service
    participant NotifyDB as Notification database

    User->>GW: POST /promotion/api/v1/flash-deals/{id}/notifications
    GW->>Promo: Forward REST/HTTPS
    Promo->>PromoDB: Kiểm tra deal FLASH và SCHEDULED
    Promo->>PromoDB: Lưu subscription idempotent
    User->>GW: POST /promotion/api/v1/flash-deals/notifications/general
    GW->>Promo: Lưu general subscription
    Scheduler->>Promo: refreshStatuses theo fixed delay
    Promo->>PromoDB: Đồng bộ status và materialize subscription chung
    Promo->>Kafka: Publish flash-sale-upcoming (trước 15 phút)
    Kafka-->>Notify: Deliver notification event
    Notify->>NotifyDB: Lưu FLASH_SALE_UPCOMING notification
```

## Activity: subscription và scheduled notification

![Activity flash deal notification](../diagrams/flash-deal-notification-activity.png)

```mermaid
flowchart TD
    start([Subscribe flash deal]) --> eligible{"Deal FLASH, SCHEDULED, startAt tương lai?"}
    eligible -->|Không| unavailable["Trả FLASH_SALE_NOTIFICATION_UNAVAILABLE"]
    eligible -->|Có| direct["Lưu subscription nếu chưa có"]
    direct --> timer["Scheduled refresh chạy"]
    timer --> materialize["Chuyển general subscription thành deal subscription"]
    materialize --> window{"Deal bắt đầu trong 15 phút và chưa notify?"}
    window -->|Không| wait["Chờ lần refresh sau"]
    window -->|Có| publish["Publish flash-sale-upcoming"]
    publish --> notification["Notification Service lưu in-app notification"]
    notification --> done([Hoàn tất])
    unavailable --> done
    wait --> done
```

## Evidence

- Subscription endpoints: `promotion-service/.../controller/FlashDealNotificationController.java`.
- Scheduler, materialization và Kafka publish: `promotion-service/.../service/implement/FlashDealServiceImpl.java`.
- Notification Kafka consumer: `notification-service/.../messaging/consumer/FlashSaleEventConsumer.java`.

## Chưa xác minh

- Tần suất `flash-deal.status-refresh-ms` là runtime configuration; code chỉ định default 60 giây.
