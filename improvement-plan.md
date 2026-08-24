# Improvement Plan - E-commerce Microservices

Cập nhật ngày: `2026-08-06`

Tài liệu này mô tả trạng thái hiện tại và thứ tự cải thiện tiếp theo của project. Roadmap cũ tập trung vào việc tạo thêm Cart, Shipping, Review và Promotion; các service đó hiện đã tồn tại, vì vậy ưu tiên mới là làm chắc luồng nghiệp vụ, khả năng chịu lỗi và quy trình delivery.

## 1. Mục tiêu

- Giữ flow mua hàng đúng khi service hoặc Kafka gặp lỗi.
- Có test tự động cho các luồng quan trọng thay vì chỉ demo thủ công.
- Quản lý schema database và secret an toàn hơn.
- Có CI/CD, container và observability đủ để vận hành toàn hệ thống.
- Chỉ thêm service mới khi các service hiện tại đã có độ tin cậy phù hợp.

## 2. Trạng thái hiện tại

### Các thành phần đã có

- API Gateway và Eureka Discovery.
- Identity/Auth, Profile và Seller.
- Product, Inventory, Search và Media.
- Cart, Wishlist và Promotion/Flash Deal.
- Order, Payment và Shipping.
- Review/Rating và Notification.
- Web application cho customer, admin, seller và shipper.
- PostgreSQL, MongoDB, Redis, Kafka và Elasticsearch.

### Các flow và nền tảng đã hoàn thành

- Inventory là source of truth của tồn kho; Product/Search giữ read model phục vụ đọc.
- Checkout từ Cart sang Order, reserve/confirm/release inventory.
- COD và Stripe payment; Stripe có webhook verification và reconciliation.
- Order xử lý `payment-success`, `payment-failed` và `payment-cancelled`.
- Promotion hỗ trợ reserve, confirm và release.
- Inventory có batch API và transactional outbox.
- Một số consumer quan trọng đã có retry/DLT và API quản trị DLT.
- HTTP/Kafka trace ID, structured logging, Logstash, Elasticsearch và Kibana đã được bổ sung cho flow chính.
- Retry/circuit breaker đã được áp dụng cho một số lời gọi liên service.

## 3. Khoảng trống chính

### 3.1. Test còn mỏng

Codebase hiện có khoảng 586 file Java production nhưng chỉ 22 file test và 33 test method. Phần lớn service mới có smoke test khởi động context; chưa có bộ integration/E2E bảo vệ toàn bộ checkout flow.

### 3.2. Kafka reliability chưa đồng đều

- Outbox mới được áp dụng rõ ở Inventory.
- Retry/DLT chưa phủ toàn bộ consumer.
- Chưa có event envelope và versioning thống nhất.
- Chưa có inbox/deduplication chung để chống xử lý event trùng.
- Chưa có reconciliation toàn tuyến cho Order, Payment, Inventory, Promotion và Shipping.

### 3.3. Database và cấu hình chưa sẵn sàng cho production

- Nhiều service vẫn dùng `ddl-auto: update`.
- Một số cấu hình local có username/password mặc định trong YAML.
- Chưa có migration versioned bằng Flyway hoặc Liquibase.
- Internal endpoint cần service-to-service authentication rõ ràng hơn.

### 3.4. Delivery chưa tự động

- Chưa có CI pipeline trong repository.
- Chỉ một phần service có Dockerfile.
- Docker Compose chủ yếu chạy infrastructure, chưa có profile chuẩn để chạy nhóm service.
- Frontend có build/lint nhưng chưa có test script.

### 3.5. Observability mới phủ flow chính

- Trace ID và structured logging chưa đồng nhất trên tất cả service.
- Chưa có metrics/alert cho consumer lag, DLT, outbox backlog và payment reconciliation.
- Chưa có SLO hoặc dashboard vận hành cho các flow quan trọng.

## 4. Nguyên tắc ưu tiên

```text
Business correctness
-> automated tests
-> event reliability
-> database/security
-> CI/CD và deployment
-> observability hoàn chỉnh
-> feature/service mới
```

Không thêm microservice chỉ để tăng số lượng service. Mỗi service mới phải có ownership dữ liệu rõ, contract rõ và lý do vận hành độc lập.

## 5. Roadmap đề xuất

### Phase 0 - Repository hygiene

Mục tiêu: Git chỉ chứa source và cấu hình có giá trị dùng chung.

- Bỏ theo dõi `.idea`; giữ cấu hình IntelliJ trên máy local.
- Bỏ lock file của `board-service` khỏi repository cho đến khi service có `package.json` và source thực tế.
- Giữ infrastructure, observability, business logic và documentation ở các commit riêng.
- Duy trì `.gitignore` cho IDE, secret và dependency sinh tự động.

Kết quả mong muốn:

```text
git status sạch, ít conflict IDE và lịch sử commit dễ review/revert.
```

### Phase 1 - Integration test cho flow mua hàng

Ưu tiên theo thứ tự:

1. Checkout COD thành công.
2. Stripe webhook thành công và webhook bị gửi trùng.
3. Không đủ tồn kho khi checkout.
4. Payment failed/cancelled và compensation tồn kho/promotion.
5. Kafka gửi event trùng nhưng consumer chỉ cập nhật một lần.
6. Timeout/downstream failure và retry/circuit breaker.
7. Shipping transition hợp lệ và không hợp lệ.

Giải pháp kỹ thuật:

- JUnit 5 và Mockito cho unit test.
- Testcontainers cho PostgreSQL, Kafka, Redis và Elasticsearch.
- Integration test theo từng service boundary.
- Một bộ E2E nhỏ chạy flow qua API Gateway.

Definition of Done:

- Happy path và failure path đều được test tự động.
- Test chạy lặp lại được trên máy mới và trong CI.
- Không phụ thuộc dữ liệu tạo tay trong database local.

### Phase 2 - Kafka reliability và consistency

- Áp dụng transactional outbox cho Order, Payment, Promotion và Shipping.
- Chuẩn hóa event envelope gồm `eventId`, `eventType`, `version`, `occurredAt`, `traceId` và business key.
- Thêm inbox/deduplication cho consumer thay đổi trạng thái hoặc số lượng.
- Chuẩn hóa retry, exponential backoff và DLT cho toàn bộ consumer.
- Có lệnh/API quản trị để xem, replay và audit DLT.
- Thêm reconciliation job cho Order-Payment-Inventory-Promotion-Shipment.
- Viết AsyncAPI hoặc tài liệu event contract và quy tắc backward compatibility.

Definition of Done:

- Producer crash sau khi commit database không làm mất event quan trọng.
- Event gửi lại không tạo cập nhật trùng.
- Event lỗi có thể quan sát và replay an toàn.

### Phase 3 - Database migration và security

- Thêm Flyway cho Order, Payment và Inventory trước; sau đó mở rộng sang các service còn lại.
- Chuyển production sang `ddl-auto: validate`.
- Tách profile `local`, `test` và `prod` rõ ràng.
- Đưa database password, JWT, Stripe, email và storage secret ra environment/secret manager.
- Bảo vệ internal API bằng service credential, mTLS hoặc network policy phù hợp.
- Kiểm tra authorization theo ownership, không chỉ theo role.

Definition of Done:

- Database trống có thể dựng hoàn toàn từ migration.
- Nâng version schema có rollback/forward strategy.
- Repository không chứa production secret.

### Phase 4 - CI/CD và container

- Thêm CI: backend compile/test, frontend lint/build và integration test chọn lọc.
- Chuẩn hóa Dockerfile multi-stage cho toàn bộ service.
- Thêm Docker Compose profile: `infra`, `core`, `observability` và `full`.
- Thêm healthcheck/readiness cho infrastructure và service.
- Thêm dependency scan, secret scan và image scan.

Definition of Done:

- Pull request lỗi build/test bị chặn tự động.
- Có thể dựng môi trường demo từ repository bằng quy trình được tài liệu hóa.

### Phase 5 - Observability hoàn chỉnh

- Phủ trace ID cho tất cả HTTP client/server, Kafka consumer/producer và scheduled job.
- Thêm Micrometer/Prometheus metrics.
- Dashboard cho latency, error rate, Kafka lag, DLT, outbox age và payment reconciliation.
- Alert cho order treo, webhook lỗi, outbox quá hạn và consumer lag cao.
- Kiểm tra log masking cho token, password, payment và dữ liệu cá nhân.

### Phase 6 - Reporting read model

Sau khi Phase 1-5 ổn định, tính năng mới có giá trị cao nhất là reporting read model cho admin:

- Doanh thu và order theo thời gian.
- Payment success/failure rate.
- Sản phẩm và seller bán chạy.
- Tồn kho thấp.
- Shipment theo trạng thái.

Reporting consume event và sở hữu database riêng; không query trực tiếp database của service khác.

## 6. Sprint tiếp theo đề xuất

Nếu chỉ chọn một sprint 1-2 tuần:

1. Hoàn tất repository hygiene.
2. Thêm CI build/test/lint cơ bản.
3. Viết integration test cho checkout COD và Stripe.
4. Test inventory failure và payment compensation.
5. Áp dụng outbox cho Payment và Order.
6. Thêm idempotency cho consumer quan trọng.
7. Bắt đầu Flyway cho Order, Payment và Inventory.

Không bắt đầu Reporting Service trong sprint này.

## 7. Cách trình bày roadmap khi phỏng vấn

```text
Project hiện đã có đầy đủ các domain service chính và flow checkout bằng COD/Stripe.
Ưu tiên tiếp theo của em không phải thêm service, mà là tăng độ tin cậy của flow
Order-Payment-Inventory: bổ sung integration test, transactional outbox,
idempotent consumer, retry/DLT và reconciliation. Sau đó em sẽ version database
bằng Flyway, tự động hóa CI/CD và hoàn thiện metrics/alerting. Khi nền tảng ổn định,
em mới xây reporting read model từ Kafka event cho dashboard admin.
```
