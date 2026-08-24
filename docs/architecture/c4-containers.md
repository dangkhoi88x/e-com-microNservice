# C4 Containers — NovaShop

Hệ thống có nhiều microservice hơn mức nên đặt vào một C4 Container diagram duy nhất. Tài liệu tách thành ba view, mỗi view chỉ giữ một ranh giới kỹ thuật rõ ràng.

## 1. Edge, identity và discovery

![Edge, identity và discovery](diagrams/c4-containers-edge-identity.png)

```mermaid
C4Container
  title Edge, identity và service discovery

  Person(user, "Người dùng", "Khách hàng, người bán hoặc admin")

  Container(web, "web-app", "React 19 + Vite", "Storefront và admin UI")
  Container(gateway, "api-gateway-service", "Spring Cloud Gateway WebFlux", "Route request và áp dụng GatewayAuthenticationFilter")
  Container(identity, "IDENTITY-SERVICE", "Spring Boot + JWT + gRPC", "Login, refresh token, JWKS và token introspection")
  Container(discovery, "discovery-server", "Netflix Eureka", "Service registry")

  Rel(user, web, "Sử dụng", "HTTPS")
  Rel(web, gateway, "Gọi public API", "HTTPS/JSON")
  Rel(gateway, identity, "Introspect bearer token", "gRPC")
  Rel(gateway, discovery, "Resolve lb:// service", "Eureka")
  Rel(identity, discovery, "Đăng ký instance", "Eureka")
```

## 2. Checkout và reservation

![Checkout và reservation](diagrams/c4-containers-checkout.png)

```mermaid
C4Container
  title Checkout và reservation

  Container(gateway, "API Gateway", "Spring Cloud Gateway", "Điểm vào HTTP")
  Container(cart, "Cart Service", "Spring Boot", "Cart và checkout item")
  Container(order, "Order Service", "Spring Boot", "Checkout, order state và orchestration")
  Container(inventory, "Inventory Service", "Spring Boot", "Stock reservation và confirmation")
  Container(promotion, "Promotion Service", "Spring Boot", "Campaign, voucher và flash deal")
  Container(product, "Product Service", "Spring Boot", "Catalog, product và variant")
  ContainerDb(orderDb, "Order persistence", "PostgreSQL", "Order và OrderItem")

  Rel(gateway, cart, "Cart API", "HTTPS/JSON")
  Rel(gateway, order, "Checkout API", "HTTPS/JSON")
  Rel(order, cart, "Cart item", "REST")
  Rel(order, product, "Product snapshot", "REST")
  Rel(order, inventory, "Reserve stock", "REST")
  Rel(order, promotion, "Reserve promotion", "REST")
  Rel(order, orderDb, "Persist order", "JPA/JDBC")
```

## 3. Payment và fulfillment

![Payment và fulfillment](diagrams/c4-containers-payment-fulfillment.png)

```mermaid
C4Container
  title Payment và fulfillment

  Container(gateway, "API Gateway", "Spring Cloud Gateway", "Điểm vào HTTP")
  Container(payment, "Payment Service", "Spring Boot", "Payment state và Stripe")
  Container(order, "Order Service", "Spring Boot", "Order state và shipment request")
  Container(inventory, "Inventory Service", "Spring Boot", "Confirm/release reservation")
  Container(shipping, "Shipping Service", "Spring Boot", "Shipment/tracking")
  ContainerQueue(kafka, "Kafka topics", "Apache Kafka", "Payment và shipment events")
  ContainerDb(paymentDb, "Payment persistence", "PostgreSQL", "Payment records")

  Rel(gateway, payment, "Route payment request", "HTTPS/JSON")
  Rel(payment, order, "Validate payment order", "REST")
  Rel(payment, paymentDb, "Persist payment", "JPA/JDBC")
  Rel(payment, kafka, "Publish payment event", "Kafka")
  Rel(kafka, order, "Deliver payment event", "Kafka")
  Rel(kafka, inventory, "Deliver payment event", "Kafka")
  Rel(order, kafka, "Publish shipment-requested", "Kafka")
  Rel(kafka, shipping, "Deliver shipment request", "Kafka")
```

## 4. Read model và supporting services

![Read model và supporting services](diagrams/c4-containers-read-support.png)

```mermaid
C4Container
  title Read model và supporting services

  Container(gateway, "API Gateway", "Spring Cloud Gateway", "Điểm vào HTTP")
  Container(product, "Product Service", "Spring Boot", "Publish product lifecycle event")
  Container(search, "Search Service", "Spring Boot", "Search read model")
  Container(review, "Review Service", "Spring Boot", "Product review và summary")
  Container(wishlist, "Wishlist Service", "Spring Boot", "Wishlist và product lookup")
  Container(profile, "Profile Service", "Spring Boot", "User profile")
  Container(notification, "Notification Service", "Spring Boot", "In-app/email notification")
  ContainerQueue(kafka, "Kafka topics", "Apache Kafka", "Event transport")
  ContainerDb(elasticsearch, "Elasticsearch", "Elasticsearch", "Product search index")
  ContainerDb(mongo, "MongoDB", "MongoDB", "Profile/notification document storage")
  ContainerDb(redis, "Redis", "Redis", "Cache/support state")

  Rel(gateway, search, "Route search request", "HTTPS/JSON")
  Rel(gateway, review, "Route review request", "HTTPS/JSON")
  Rel(gateway, wishlist, "Route wishlist request", "HTTPS/JSON")
  Rel(product, kafka, "Publish product-created/product-updated", "Kafka")
  Rel(kafka, search, "Consume product event", "Kafka")
  Rel(search, elasticsearch, "Index và query", "Elasticsearch API")
  Rel(kafka, notification, "Consume order/shipment/profile event", "Kafka")
  Rel(profile, mongo, "Persist", "MongoDB")
  Rel(notification, mongo, "Persist", "MongoDB")
  Rel(wishlist, product, "Lấy product snapshot", "REST")
  Rel(product, redis, "Cache support", "Redis")
```

## 5. Seller và media

![Seller và media services](diagrams/c4-containers-seller-media.png)

```mermaid
C4Container
  title Seller và media services

  Container(gateway, "API Gateway", "Spring Cloud Gateway", "Điểm vào HTTP")
  Container(seller, "Seller Service", "Spring Boot", "Seller shop và eligibility")
  Container(product, "Product Service", "Spring Boot", "Seller product command")
  Container(media, "Media Service", "Spring Boot", "Media asset")
  ContainerDb(postgres, "PostgreSQL", "PostgreSQL", "Seller/product/media persistence")
  System_Ext(s3, "S3-compatible storage", "Media object storage")

  Rel(gateway, seller, "Route seller request", "HTTPS/JSON")
  Rel(gateway, product, "Route seller product request", "HTTPS/JSON")
  Rel(gateway, media, "Route media request", "HTTPS/JSON")
  Rel(product, seller, "Verify seller eligibility", "REST")
  Rel(seller, postgres, "Persist", "JPA/JDBC")
  Rel(product, postgres, "Persist", "JPA/JDBC")
  Rel(media, postgres, "Persist metadata", "JPA/JDBC")
  Rel(media, s3, "Store/retrieve object", "S3 API")
```

## Evidence

- Gateway route map: `api-gateway-service/.../configuration/GatewayConfiguration.java`.
- Synchronous clients: `order-service/.../client/*.java`, `payment-service/.../client/OrderClient.java`, `wishlist-service/.../client/ProductClient.java`.
- Kafka: `OrderServiceImpl.java`, `PaymentServiceImpl.java`, `ProductServiceImpl.java`, `ProductEventConsumer.java`, `PaymentEventConsumer.java`, `ShipmentRequestedConsumer.java`.
- Infra: `docker-compose.yaml`.
