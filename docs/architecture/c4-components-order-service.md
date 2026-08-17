# C4 Component — Order Service

Order Service là component quan trọng nhất của checkout: lấy cart items, lấy product snapshot, điều phối reserve inventory/promotion và lưu trạng thái order. Nó phát event thay vì trực tiếp tạo shipment.

![Component diagram Order Service](diagrams/c4-components-order-service.png)

```mermaid
C4Component
  title Component diagram — Order Service

  Container_Ext(gateway, "API Gateway", "Spring Cloud Gateway", "Route /order/**")
  Container_Ext(cart, "Cart Service", "Spring Boot", "Cart checkout/mark/finalize")
  Container_Ext(product, "Product Service", "Spring Boot", "Product snapshot")
  Container_Ext(inventory, "Inventory Service", "Spring Boot", "Stock reservation")
  Container_Ext(promotion, "Promotion Service", "Spring Boot", "Promotion/flash-deal reservation")
  ContainerQueue(kafka, "Kafka", "Apache Kafka", "Order and shipment topics")
  ContainerDb(orderDb, "Order persistence", "PostgreSQL", "Order và OrderItem")

  Component(controller, "OrderController", "Spring MVC", "Public REST endpoints, JWT principal")
  Component(service, "OrderServiceImpl", "Spring service", "Checkout, state transition và compensation")
  Component(repo, "OrderRepository", "Spring Data JPA", "Persist Order aggregate")
  Component(productClient, "ProductClient", "WebClient", "Lấy product/variant snapshot")
  Component(cartClient, "CartClient", "WebClient", "Lấy và lock/finalize cart item")
  Component(inventoryClient, "InventoryClient", "WebClient", "Reserve/release/confirm inventory")
  Component(promotionClient, "PromotionClient", "WebClient", "Reserve/confirm/release promotion")
  Component(paymentConsumer, "PaymentEventConsumer", "Kafka listener", "Xử lý payment event")

  Rel(gateway, controller, "Forward request", "HTTPS/JSON")
  Rel(controller, service, "Gọi use case", "Java")
  Rel(service, repo, "Lưu/đọc order", "JPA")
  Rel(repo, orderDb, "Persist", "JDBC")
  Rel(service, productClient, "Lấy product", "REST")
  Rel(productClient, product, "GET product detail", "REST/HTTPS")
  Rel(service, cartClient, "Lấy/mark/finalize item", "REST")
  Rel(cartClient, cart, "Gọi Cart API", "REST/HTTPS")
  Rel(service, inventoryClient, "Reserve/release/confirm", "REST")
  Rel(inventoryClient, inventory, "Gọi internal inventory API", "REST/HTTPS")
  Rel(service, promotionClient, "Reserve/release promotion", "REST")
  Rel(promotionClient, promotion, "Gọi internal promotion API", "REST/HTTPS")
  Rel(service, kafka, "Publish order-created/order-status-updated", "Kafka")
  Rel(kafka, paymentConsumer, "Deliver payment-success/failed/cancelled", "Kafka")
  Rel(paymentConsumer, service, "Gọi state transition", "Java")
```

## Trạng thái và resilience đã thấy trong code

- `createOrder` tạo aggregate rồi reserve inventory trước promotion/flash deal. Khi bước sau lỗi, service gọi compensation release tương ứng và lưu status `INVENTORY_FAILED` hoặc `PROMOTION_FAILED`.
- `checkout` lấy selected items qua `CartClient.checkoutItems`, gọi `createOrder`, rồi mark cart item khi order ở `PENDING_PAYMENT`.
- `PaymentEventConsumer` nghe `payment-success`, `payment-failed`, `payment-cancelled` và `payment-cod-created` để cập nhật order; `OrderServiceImpl` publish `shipment-requested` khi order bắt đầu shipping.

## Evidence

- API: `order-service/src/main/java/com/example/orderservice/controller/OrderController.java`.
- Orchestration/state/event: `order-service/src/main/java/com/example/orderservice/service/implement/OrderServiceImpl.java`.
- Payment consumers: `order-service/src/main/java/com/example/orderservice/messaging/consumer/PaymentEventConsumer.java`.
- Clients: `order-service/src/main/java/com/example/orderservice/client/`.
