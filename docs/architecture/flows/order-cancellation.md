# Flow — Huỷ order và huỷ shipment liên quan

## Scope

Người dùng huỷ order qua Order Service nếu trạng thái thuộc tập được phép huỷ. Với order `PENDING_PAYMENT`, service giải phóng inventory/promotion/flash deal và cart trước khi lưu `CANCELLED`. Event `order-cancelled` được Shipping Service và Notification Service consume; Shipping chỉ huỷ shipment đang ở các trạng thái cho phép rồi phát `shipment-status-updated`.

![Sequence huỷ order và shipment](../diagrams/order-cancellation-sequence.png)

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant GW as API Gateway
    participant Order as Order Service
    participant Inventory as Inventory Service
    participant Promo as Promotion Service
    participant Cart as Cart Service
    participant OrderDB as Order database
    participant Kafka as Kafka
    participant Shipping as Shipping Service
    participant Notify as Notification Service

    User->>GW: PUT /order/api/v1/orders/{id}/cancel
    GW->>Order: Forward REST/HTTPS + bearer token
    Order->>OrderDB: Tìm order theo userId
    alt Order PENDING_PAYMENT
        Order->>Inventory: Release reservation (REST)
        Order->>Promo: Release promotion/flash deal (REST)
        Order->>Cart: Release checked-out cart item (REST)
    end
    Order->>OrderDB: Lưu order CANCELLED
    Order->>Kafka: Publish order-cancelled
    Kafka-->>Shipping: Deliver OrderCancelledEvent
    Shipping->>Shipping: Huỷ shipment nếu trạng thái được phép
    Shipping->>Kafka: Publish shipment-status-updated
    Kafka-->>Notify: Deliver order-cancelled
    Notify->>Notify: Lưu cancellation notification
    Order-->>User: 200 Cancelled
```

## Activity: cancellation và shipment guard

![Activity huỷ order và shipment](../diagrams/order-cancellation-activity.png)

```mermaid
flowchart TD
    start([Yêu cầu huỷ order]) --> found{"Order thuộc user và được phép huỷ?"}
    found -->|Không| error["Trả ORDER_NOT_FOUND hoặc ORDER_CANNOT_BE_CANCELLED"]
    found -->|Có| pending{"Status PENDING_PAYMENT?"}
    pending -->|Có| release["Release inventory, promotion/flash deal, cart"]
    pending -->|Không| cancelOrder["Lưu Order CANCELLED"]
    release --> cancelOrder
    cancelOrder --> event["Publish order-cancelled"]
    event --> shipment{"Có shipment ở trạng thái huỷ được?"}
    shipment -->|Có| cancelShipment["Shipping lưu CANCELLED và publish status"]
    shipment -->|Không| notify["Notification lưu thông báo huỷ order"]
    cancelShipment --> notify
    notify --> done([Hoàn tất])
    error --> done
```

## Evidence

- Cancel endpoint: `order-service/.../controller/OrderController.java`.
- Release dependencies và event: `order-service/.../service/implement/OrderServiceImpl.java` (`cancelOrder`).
- Shipping consumer/cancellation: `shipping-service/.../messaging/consumer/OrderCancelledConsumer.java`, `service/implement/ShipmentServiceImpl.java`.
- Notification consumer: `notification-service/.../messaging/consumer/OrderEventConsumer.java`.

## Chưa xác minh

- Khi một release REST call lỗi, transaction/compensation chi tiết cần kiểm tra thêm tại integration test; sơ đồ không suy đoán distributed transaction.
