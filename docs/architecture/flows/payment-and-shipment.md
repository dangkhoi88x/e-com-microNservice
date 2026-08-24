# Flow — Payment, fulfillment và shipment

## Scope

Payment Service tạo payment cho order `PENDING_PAYMENT`. Với Stripe, service tạo Checkout Session và xử lý webhook/reconcile. Khi payment thành công, Kafka event được Order và Inventory consumer xử lý; Order Service publish `shipment-requested`, sau đó Shipping Service tạo shipment.

![Sequence payment, fulfillment và shipment](../diagrams/payment-and-shipment-sequence.png)

```mermaid
sequenceDiagram
    actor User as Khách hàng
    participant UI as web-app
    participant GW as API Gateway
    participant Pay as Payment Service
    participant Order as Order Service
    participant Stripe as Stripe
    participant Kafka as Kafka
    participant Inventory as Inventory Service
    participant Cart as Cart Service
    participant Promo as Promotion Service
    participant Shipping as Shipping Service

    User->>UI: Chọn phương thức thanh toán
    UI->>GW: POST /payment/api/v1/payments
    GW->>Pay: Forward REST/HTTPS
    Pay->>Order: GET internal payment validation (REST)
    Order-->>Pay: Order PENDING_PAYMENT
    Pay-->>UI: Payment PENDING
    opt Phương thức Stripe
        UI->>GW: POST /payment/api/v1/payments/{id}/stripe-checkout
        GW->>Pay: Forward request
        Pay->>Stripe: Tạo Checkout Session (HTTPS)
        Stripe-->>Pay: Checkout URL/session
        Stripe->>GW: POST webhook hoặc user reconcile
        GW->>Pay: Deliver webhook
    end
    Pay->>Kafka: Publish payment-success
    Kafka-->>Order: Deliver payment-success
    Order->>Order: Confirm order và finalize payment success
    Order->>Cart: Finalize checked-out cart items (REST)
    Order->>Promo: Confirm promotion/flash deal (REST)
    Order->>Kafka: Publish shipment-requested
    Kafka-->>Inventory: Deliver payment-success
    Inventory->>Inventory: Confirm inventory reservation
    Kafka-->>Shipping: Deliver shipment-requested
    Shipping->>Shipping: Tạo shipment
```

## Activity: nhánh success và failure

![Activity payment success và failure](../diagrams/payment-and-shipment-activity.png)

```mermaid
flowchart TD
    start([Payment PENDING]) --> method{"Phương thức?"}
    method -->|Stripe| checkout["Tạo Stripe Checkout Session"]
    checkout --> paid{"Webhook/reconcile paid?"}
    method -->|COD| cod["Tạo COD payment event"]
    paid -->|Có| success["Lưu SUCCESS và publish payment-success"]
    paid -->|Không hoặc hết hạn| failure["Lưu FAILED/CANCELLED và publish event"]
    cod --> shipping["Order chuyển SHIPPING và publish shipment-requested"]
    success --> confirmOrder["Order consumer confirm order"]
    confirmOrder --> confirmStock["Inventory consumer confirm reservation"]
    confirmStock --> shipping
    failure --> releaseStock["Inventory consumer release reservation"]
    releaseStock --> releaseBusiness["Order release promotion/flash deal/cart state"]
    shipping --> createShipment["Shipping consumer tạo shipment"]
    createShipment --> finish([Kết thúc])
    releaseBusiness --> finish
```

## Evidence

- Payment API/Stripe entry points: `payment-service/src/main/java/com/example/paymentservice/controller/PaymentController.java`.
- Payment status/event publisher: `payment-service/src/main/java/com/example/paymentservice/service/implement/PaymentServiceImpl.java`.
- Order payment consumer/state transition: `order-service/src/main/java/com/example/orderservice/messaging/consumer/PaymentEventConsumer.java`, `service/implement/OrderServiceImpl.java`.
- Inventory consumer: `inventory-service/src/main/java/com/example/inventoryservice/messaging/consumer/PaymentEventConsumer.java`.
- Shipment consumer: `shipping-service/src/main/java/com/example/shippingservice/messaging/consumer/ShipmentRequestedConsumer.java`.

## Chưa xác minh

- Stripe webhook public reachability và production signature/secret provisioning cần xác minh tại runtime/deployment configuration.
- Nội dung/exact ordering của notification sau payment success không được gộp vào sequence này, vì consumer notification có nhiều topic riêng.
