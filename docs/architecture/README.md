# Kiến trúc NovaShop

## Phạm vi và nguyên tắc

Tài liệu này phản ánh code và cấu hình đang có trong repository tại thời điểm tạo. Nó không chứa giá trị của bất kỳ password, token, access key, JDBC credential, SMTP credential hoặc khoá Stripe nào. Mọi cấu hình nhạy cảm chỉ được tham chiếu bằng tên biến môi trường, ví dụ `JWT_ISSUER`, `JWT_JWK_SET_URI`, `KAFKA_BOOTSTRAP_SERVERS`, `STRIPE_SECRET_KEY` và các biến `SPRING_DATASOURCE_*`.

## Tổng quan

NovaShop là một e-commerce microservices workspace. Root Maven POM là một aggregator của 17 Spring Boot module; React/Vite trong `web-app` là frontend. API Gateway là điểm vào HTTP cho frontend, tra cứu route thông qua Eureka và xác thực request bảo vệ bằng gRPC token introspection đến Identity Service. Các service dùng REST/WebClient cho lệnh cần phản hồi ngay và Kafka cho event bất đồng bộ.

Xem các sơ đồ theo thứ tự sau:

- [C4 Context](c4-context.md)
- [C4 Containers](c4-containers.md)
- [Component: Order Service](c4-components-order-service.md)
- [Component: Product Service](c4-components-product-service.md)
- [Local deployment](c4-deployment.md)
- [Checkout và reservation](flows/checkout-reservation.md)
- [Payment, fulfillment và shipment](flows/payment-and-shipment.md)
- [Product và search indexing](flows/product-search-indexing.md)
- [Tạo user, profile và welcome notification](flows/user-registration-profile.md)
- [Đặt lại mật khẩu](flows/password-reset.md)
- [Review summary và search](flows/review-summary-search.md)
- [Duyệt và suspend seller shop](flows/seller-shop-governance.md)
- [Huỷ order và shipment](flows/order-cancellation.md)
- [Upload media asset](flows/media-upload.md)
- [Login, refresh token và logout](flows/authentication-session.md)
- [Seller product lifecycle và search indexing](flows/seller-product-lifecycle.md)
- [Claim promotion và reservation theo order](flows/promotion-claim-reservation.md)
- [Thông báo flash deal sắp diễn ra](flows/flash-deal-notification.md)
- [Shipment tracking, delivery và return](flows/shipment-lifecycle.md)
- [Thêm product vào cart hoặc wishlist](flows/cart-wishlist.md)

## Tech stack đã xác minh

| Lớp | Công nghệ / cơ chế | Bằng chứng |
| --- | --- | --- |
| Backend | Java 25, Maven, Spring Boot, Spring Cloud | `api-gateway-service/pom.xml`, các `*/pom.xml` |
| Gateway | Spring Cloud Gateway WebFlux | `api-gateway-service/pom.xml`, `GatewayConfiguration.java` |
| Service discovery | Netflix Eureka | `discovery-server/pom.xml`, các `application.yaml` |
| Security | JWT/OAuth2 Resource Server; Gateway introspection bằng gRPC | `GatewayAuthenticationFilter.java`, `IntrospectGrpcClient.java` |
| Synchronous calls | HTTP REST với `WebClient` hoặc `RestClient`; gRPC ở Gateway → Identity | các `*Client.java` của Order, Payment, Cart, Wishlist, Product; `IntrospectGrpcClient.java` |
| Async calls | Apache Kafka | `spring-boot-starter-kafka` trong POM; `@KafkaListener` và `KafkaTemplate` |
| Data | PostgreSQL, MongoDB, Redis, Elasticsearch | `docker-compose.yaml`; POM/config service |
| Frontend | React 19, Vite 7, Axios, React Router, MUI/Tailwind | `web-app/package.json` |

## Module và trách nhiệm

| Module / runtime | Trách nhiệm suy ra từ code | Port cấu hình local |
| --- | --- | ---: |
| `api-gateway-service` | Định tuyến public API, CORS và token introspection | 9191 |
| `discovery-server` | Eureka service registry | 8761 |
| `Microservice-ecom` / `IDENTITY-SERVICE` | Đăng ký, login, refresh token, JWT/JWKS và identity | 8090 |
| `profile-service` | Profile người dùng | 8081 |
| `notification-service` | In-app notification và email template/consumer | 8083 |
| `product-service` | Catalog, category, product, variant và product events | 8084 |
| `order-service` | Checkout, order lifecycle, reservation orchestration | 8086 |
| `inventory-service` | Stock, reservation, confirm/release tồn kho | 8087 |
| `payment-service` | Payment, Stripe checkout/webhook và payment events | 8088 |
| `cart-service` | Cart và trạng thái item khi checkout | 8089 |
| `wishlist-service` | Wishlist và product snapshot lookup | 8092 |
| `search-service` | Search read model và consumer product event | 8093 |
| `promotion-service` | Promotion campaign, voucher, flash deal, reserve/confirm/release | 8095 |
| `shipping-service` | Tạo shipment, tracking và shipment events | 8096 |
| `review-service` | Product review, eligibility check và review summary event | 8097 |
| `seller-service` | Seller shop và seller eligibility | 8098 |
| `media-service` | Media asset và S3-compatible object storage integration | Chưa xác minh |
| `web-app` | Storefront và admin web UI | Vite default/dev config |

`board-service` là một directory trong repository nhưng không nằm trong `<modules>` của root `pom.xml` và không có `application.yaml` được phát hiện trong khảo sát này; vì vậy nó không được mô hình hoá như runtime service. **Chưa xác minh:** cần kiểm tra owner/module build của `board-service` nếu đây là service dự kiến chạy.

## Storage và external systems

| Thành phần | Vai trò đã xác minh | Owner/consumer chính |
| --- | --- | --- |
| PostgreSQL | Persist catalog, cart/order/payment và các DB service riêng được Compose khai báo | Product, Cart, Order, Payment, Inventory, Promotion, Shipping, Review, Seller, Wishlist, Media |
| MongoDB | Document persistence | Profile, Notification |
| Redis | Cache/support state | Identity; Product và Promotion có dependency Redis |
| Kafka | Event bus local | Order, Payment, Product, Inventory, Shipping, Search, Notification và các consumer khác |
| Elasticsearch | Search index/read model | Search Service |
| Stripe | External online-payment provider | Payment Service |
| S3-compatible storage | Object/media storage | Media Service |
| SMTP | Gửi email template | Notification Service |

## Cách chạy local

1. Cài JDK 25, Maven/Maven Wrapper, Node.js LTS và Docker Desktop.
2. Khởi động Docker Compose cho hạ tầng cần dùng. Compose root định nghĩa PostgreSQL, MongoDB, Redis, Kafka, Elasticsearch, Logstash và Kibana; đồng thời có container definition cho một số application service.
3. Chạy `discovery-server`, sau đó Identity Service và các business service cần cho use case. Kiểm tra trạng thái đăng ký tại Eureka trước khi gọi qua Gateway.
4. Chạy `web-app` với `npm ci` rồi `npm run dev`.

Ví dụ các lệnh không chứa secret:

```powershell
docker compose up -d kafka redis mongodb elasticsearch
mvn -f discovery-server\pom.xml spring-boot:run
mvn -f Microservice-ecom\pom.xml spring-boot:run
mvn -f api-gateway-service\pom.xml spring-boot:run
Set-Location web-app
npm ci
npm run dev
```

Tùy service, cần cung cấp environment variables đã được cấu hình cho môi trường local. Không commit file `.env.dev` chứa giá trị thật.

## Giao tiếp chính

- `GatewayConfiguration` route `/order/**`, `/payment/**`, `/product/**`, `/search/**`, `/inventory/**` và nhiều public path khác tới service theo Eureka `lb://...`.
- `GatewayAuthenticationFilter` bỏ qua một danh sách endpoint public; các endpoint khác cần `Authorization: Bearer ...`, được Gateway introspect qua gRPC.
- `OrderServiceImpl` gọi Product, Inventory, Cart và Promotion bằng HTTP trong checkout, sau đó publish `order-created` và order status events.
- `PaymentServiceImpl` xác nhận order bằng HTTP, publish `payment-success`, `payment-failed`, `payment-cancelled` hoặc `payment-cod-created` qua Kafka.
- `ProductServiceImpl` publish `product-created` và `product-updated`; `ProductEventConsumer` của Search Service cập nhật Elasticsearch.

## Evidence

| Nhận định | File/class xác minh |
| --- | --- |
| Root gồm 17 Maven module | `pom.xml` |
| Tên service và port | `*/src/main/resources/application.yaml` |
| Gateway routes | `api-gateway-service/src/main/java/com/example/apigatewayservice/configuration/GatewayConfiguration.java` |
| Gateway gRPC introspection | `GatewayAuthenticationFilter.java`, `grpc/IntrospectGrpcClient.java` |
| Checkout orchestration | `order-service/.../OrderController.java`, `service/implement/OrderServiceImpl.java`, `client/InventoryClient.java`, `client/CartClient.java`, `client/PromotionClient.java` |
| Payment/Stripe/event | `payment-service/.../PaymentController.java`, `service/implement/PaymentServiceImpl.java` |
| Inventory payment reaction | `inventory-service/.../messaging/consumer/PaymentEventConsumer.java` |
| Shipment request handling | `shipping-service/.../messaging/consumer/ShipmentRequestedConsumer.java` |
| Search indexing | `product-service/.../ProductServiceImpl.java`, `search-service/.../messaging/ProductEventConsumer.java` |
| User/profile provisioning | `Microservice-ecom/.../UserService.java`, `messaging/producer/UserCreatedEventKafkaPublisher.java`, `profile-service/.../UserProfileConsumer.java` |
| Password reset | `Microservice-ecom/.../PasswordResetService.java`, `client/PasswordResetMailClient.java`, `notification-service/.../InternalMailController.java` |
| Review summary indexing | `review-service/.../ReviewServiceImpl.java`, `messaging/ReviewSummaryEventPublisher.java`, `search-service/.../ReviewSummaryEventConsumer.java` |
| Seller shop governance | `seller-service/.../SellerShopServiceImpl.java`, `product-service/.../SellerShopEventConsumer.java` |
| Order cancellation | `order-service/.../OrderServiceImpl.java`, `shipping-service/.../OrderCancelledConsumer.java`, `notification-service/.../OrderEventConsumer.java` |
| Media object lifecycle | `media-service/.../MediaController.java`, `service/MediaAssetService.java` |
| Authentication/session | `Microservice-ecom/.../AuthenticationController.java`, `service/AuthenticationService.java` |
| Seller product lifecycle | `product-service/.../SellerProductController.java`, `service/implement/ProductServiceImpl.java`, `search-service/.../ProductEventConsumer.java` |
| Promotion usage | `promotion-service/.../PromotionClaimController.java`, `service/implement/PromotionUsageServiceImpl.java` |
| Flash deal notification | `promotion-service/.../FlashDealServiceImpl.java`, `notification-service/.../FlashSaleEventConsumer.java` |
| Shipment lifecycle | `shipping-service/.../ShipmentController.java`, `service/implement/ShipmentServiceImpl.java` |
| Cart/wishlist snapshots | `cart-service/.../CartServiceImpl.java`, `wishlist-service/.../WishlistServiceImpl.java` |
| Local infrastructure | `docker-compose.yaml`, `elk/logstash/pipeline/logstash.conf` |

## Điểm chưa xác minh

- Production topology, cloud account, namespace Kubernetes, CI/CD pipeline và secret manager không có manifest/pipeline đủ bằng chứng trong phạm vi đã đọc.
- `media-service` không khai báo `server.port` trong `application.yaml` hiện có; cần xác nhận port runtime hoặc biến môi trường override trước khi đưa vào runbook.
- Compose là local topology; không suy ra nó đại diện cho production.
