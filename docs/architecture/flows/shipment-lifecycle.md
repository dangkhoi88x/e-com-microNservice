# Flow — Shipment tracking, delivery và return

## Scope

Admin gán carrier, chuyển shipment qua `PACKING`, `READY_TO_SHIP`, `IN_TRANSIT`; admin/shipper có thể mark `DELIVERED`, `DELIVERY_FAILED`, `RETURNING`, `RETURNED`. Mỗi status transition lưu ShipmentHistory và phát `shipment-status-updated`; Order Service cập nhật order theo event, Notification Service lưu thông báo cho user.

![Sequence shipment lifecycle](../diagrams/shipment-lifecycle-sequence.png)

```mermaid
sequenceDiagram
    actor Operator as Admin hoặc shipper
    participant GW as API Gateway
    participant Shipping as Shipping Service
    participant ShipDB as Shipping database
    participant Kafka as Kafka
    participant Order as Order Service
    participant OrderDB as Order database
    participant Notify as Notification Service

    Operator->>GW: PUT /shipping/api/v1/shipments/{id}/ship
    GW->>Shipping: Forward REST/HTTPS + bearer token
    Shipping->>ShipDB: Kiểm tra status/carrier/tracking
    Shipping->>ShipDB: Lưu IN_TRANSIT và ShipmentHistory
    Shipping->>Kafka: Publish shipment-status-updated
    Kafka-->>Order: Deliver ShipmentStatusUpdatedEvent
    Order->>OrderDB: Update order state từ shipment event
    Kafka-->>Notify: Deliver ShipmentStatusUpdatedEvent
    Notify->>Notify: Lưu shipment notification
    opt Delivery failed
        Operator->>Shipping: PUT delivery-failed, returning, returned
        Shipping->>ShipDB: Lưu transition + history
        Shipping->>Kafka: Publish status event mỗi transition
    end
```

## Activity: shipment state machine

![Activity shipment lifecycle](../diagrams/shipment-lifecycle-activity.png)

```mermaid
flowchart TD
    start([Shipment CREATED]) --> packing["PACKING"]
    packing --> ready["READY_TO_SHIP"]
    ready --> carrier{"Có carrier và tracking?"}
    carrier -->|Không| invalid["Trả INVALID_REQUEST"]
    carrier -->|Có| transit["IN_TRANSIT"]
    transit --> delivered{"Giao thành công?"}
    delivered -->|Có| doneStatus["DELIVERED"]
    delivered -->|Không| failed["DELIVERY_FAILED"]
    failed --> returning["RETURNING"]
    returning --> returned["RETURNED"]
    doneStatus --> event["Lưu history và publish status event"]
    returned --> event
    event --> consumers["Order update và Notification consumer"]
    consumers --> done([Hoàn tất])
    invalid --> done
```

## Evidence

- Role-protected state endpoints: `shipping-service/.../controller/ShipmentController.java`.
- Valid transitions, history and Kafka publisher: `shipping-service/.../service/implement/ShipmentServiceImpl.java`.
- Order consumer: `order-service/.../messaging/consumer/ShipmentEventConsumer.java`.
- Notification consumer: `notification-service/.../messaging/consumer/ShipmentEventConsumer.java`.

## Chưa xác minh

- Mapping đầy đủ từ từng shipment status sang `OrderStatus` cần kiểm tra phần `updateOrderFromShipment` cùng test scenario.
