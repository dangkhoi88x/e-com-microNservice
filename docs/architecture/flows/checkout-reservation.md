# Flow — Checkout và reservation

## Scope

Flow này mô tả `POST /api/v1/orders/checkout`. Nó chỉ mô tả bước đã có trong `OrderServiceImpl`: đọc selected cart items, snapshot product/variant, persist Order, reserve inventory/flash deal/promotion, chuyển order sang `PENDING_PAYMENT`, đánh dấu cart item và publish event.

![Sequence checkout và reservation](../diagrams/checkout-reservation-sequence.png)

```mermaid
sequenceDiagram
    actor User as Khách hàng
    participant UI as web-app
    participant GW as API Gateway
    participant Order as Order Service
    participant Cart as Cart Service
    participant Product as Product Service
    participant Inventory as Inventory Service
    participant Promo as Promotion Service
    participant OrderDB as Order PostgreSQL
    participant Kafka as Kafka
    participant Notify as Notification Service

    User->>UI: Chọn cart item và checkout
    UI->>GW: POST /order/api/v1/orders/checkout
    GW->>Order: Forward REST/HTTPS + bearer token
    Order->>Cart: GET selected checkout items (REST)
    Cart-->>Order: CartItem list
    loop Mỗi item
        Order->>Product: GET product/variant snapshot (REST)
        Product-->>Order: Product detail
    end
    Order->>OrderDB: Lưu Order PENDING (JPA/JDBC)
    Order->>Inventory: POST internal reserve (REST)
    Inventory-->>Order: Reservation result
    Order->>Promo: Reserve flash deal/promotion (REST)
    Promo-->>Order: Price/reservation result
    Order->>OrderDB: Lưu PENDING_PAYMENT
    Order->>Cart: Mark selected item theo order (REST)
    Order->>Kafka: Publish order-created và order-status-updated
    Kafka-->>Notify: Deliver order-created
    Notify-->>Notify: Tạo notification
    Order-->>GW: Order response
    GW-->>UI: 201 Created
```

## Activity và compensation

![Activity checkout và compensation](../diagrams/checkout-reservation-activity.png)

```mermaid
flowchart TD
    start([Bắt đầu checkout]) --> cart["Lấy selected CartItem"]
    cart --> empty{"Cart rỗng?"}
    empty -->|Có| error["Trả CART_CHECKOUT_EMPTY"]
    empty -->|Không| snapshot["Lấy product/variant snapshot và tính subtotal"]
    snapshot --> valid{"Product active và cùng seller?"}
    valid -->|Không| productError["Trả lỗi product hoặc multi-shop checkout"]
    valid -->|Có| savePending["Lưu Order PENDING"]
    savePending --> reserveStock["Reserve inventory"]
    reserveStock --> reservePromo["Reserve flash deal/promotion"]
    reservePromo --> success["Lưu PENDING_PAYMENT, mark cart và publish event"]
    reservePromo -->|Lỗi sau stock reserve| compensate["Release inventory và promotion/flash deal đã reserve"]
    reserveStock -->|Lỗi| inventoryFailed["Lưu INVENTORY_FAILED"]
    compensate --> promoFailed["Lưu PROMOTION_FAILED"]
    success --> finish([Kết thúc])
    inventoryFailed --> finish
    promoFailed --> finish
```

## Evidence

- Entry point: `order-service/src/main/java/com/example/orderservice/controller/OrderController.java` (`@PostMapping("/checkout")`).
- Orchestration: `order-service/src/main/java/com/example/orderservice/service/implement/OrderServiceImpl.java` (`checkout`, `createOrder`, `reserveInventory`, compensation helpers).
- REST clients: `order-service/src/main/java/com/example/orderservice/client/CartClient.java`, `ProductClient.java`, `InventoryClient.java`, `PromotionClient.java`.
- Notification subscription: `notification-service/src/main/java/com/example/notificationservice/messaging/consumer/OrderEventConsumer.java`.

## Chưa xác minh

Timeout/retry policy của từng HTTP dependency cần xác minh từ runtime configuration hoặc integration test; flow này không giả định distributed transaction.
