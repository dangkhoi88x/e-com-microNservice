# Khoi Microservice E-commerce

Đây là project backend e-commerce được xây dựng theo kiến trúc microservices.

Nói đơn giản, project này mô phỏng một hệ thống bán hàng online gồm nhiều phần nhỏ như:

- Đăng ký, đăng nhập user.
- Quản lý thông tin cá nhân.
- Quản lý sản phẩm.
- Tìm kiếm sản phẩm.
- Quản lý tồn kho.
- Tạo đơn hàng.
- Thanh toán.
- Gửi thông báo.

Thay vì viết tất cả vào một ứng dụng lớn, project chia thành nhiều service nhỏ. Mỗi service chịu trách nhiệm một phần nghiệp vụ riêng.

Ví dụ:

```text
Product Service chỉ lo thông tin sản phẩm.
Inventory Service chỉ lo tồn kho.
Order Service chỉ lo đơn hàng.
Payment Service chỉ lo thanh toán.
Search Service chỉ lo tìm kiếm.
```

Lợi ích của cách chia này là hệ thống dễ mở rộng, dễ sửa từng phần, và giống cách nhiều hệ thống thật ngoài công ty đang làm.

## Dành Cho Người Chưa Biết Code

Hãy tưởng tượng hệ thống này giống một cửa hàng online lớn.

Trong cửa hàng đó có nhiều bộ phận:

| Bộ phận ngoài đời | Service trong project | Công việc |
| --- | --- | --- |
| Quầy đăng ký thành viên | Identity Service | Đăng ký, đăng nhập, cấp token |
| Hồ sơ khách hàng | Profile Service | Lưu tên, email, thông tin cá nhân |
| Quầy trưng bày sản phẩm | Product Service | Lưu tên sản phẩm, giá, hình ảnh, trạng thái |
| Kho hàng | Inventory Service | Biết chính xác còn bao nhiêu hàng |
| Nhân viên nhận đơn | Order Service | Tạo đơn hàng và giữ trạng thái đơn |
| Quầy thu ngân | Payment Service | Xử lý thanh toán |
| Công cụ tìm kiếm | Search Service | Tìm sản phẩm nhanh |
| Nhân viên gửi email | Notification Service | Gửi thông báo/email |
| Cổng ra vào | API Gateway | Nhận request từ client rồi chuyển đúng service |
| Bảng danh bạ nội bộ | Discovery Server | Giúp các service tìm thấy nhau |

Khi khách mua hàng, các bộ phận này phối hợp với nhau.

Ví dụ:

```text
Khách chọn sản phẩm
-> hệ thống kiểm tra sản phẩm còn bán không
-> hệ thống hỏi kho còn hàng không
-> nếu còn hàng thì giữ hàng lại
-> khách thanh toán
-> nếu thanh toán thành công thì đơn được xác nhận
-> kho chuyển hàng từ "đang giữ" sang "đã bán"
```

## Project Này Giải Quyết Bài Toán Gì?

Project mô phỏng flow e-commerce thực tế:

1. Seller tạo sản phẩm.
2. Admin/Seller tạo tồn kho cho sản phẩm.
3. User xem danh sách sản phẩm.
4. User tìm kiếm sản phẩm.
5. User tạo đơn hàng.
6. Hệ thống giữ hàng trong kho.
7. User thanh toán.
8. Nếu thanh toán thành công, đơn hàng được xác nhận.
9. Nếu thanh toán thất bại hoặc bị hủy, hàng được trả lại kho.
10. Search/catalog được cập nhật lại tình trạng còn hàng/hết hàng.

Điểm quan trọng nhất của project hiện tại là phần tồn kho:

```text
Inventory Service là nơi quyết định số lượng hàng thật.
Product Service chỉ giữ bản sao quantity để hiển thị nhanh.
Search Service chỉ giữ inStock để tìm kiếm nhanh.
```

Điều này giúp tránh lỗi phổ biến trong microservices: mỗi service tự giữ một số lượng hàng khác nhau rồi dữ liệu bị lệch.

## Tech Stack

| Thành phần | Công nghệ | Giải thích dễ hiểu |
| --- | --- | --- |
| Backend | Java, Spring Boot | Nền tảng chính để viết API |
| Service Discovery | Eureka | Danh bạ để các service tìm nhau |
| API Gateway | Spring Cloud Gateway | Cổng chính nhận request từ client |
| Database | PostgreSQL, MongoDB | Nơi lưu dữ liệu |
| Cache | Redis | Bộ nhớ nhanh |
| Message Broker | Kafka | Hàng đợi sự kiện giữa các service |
| Search Engine | Elasticsearch | Tìm kiếm sản phẩm nhanh |
| Security | JWT, Spring Security | Xác thực và phân quyền |
| Build Tool | Maven Wrapper | Công cụ build/chạy project |
| Local Runtime | Docker Compose | Chạy database, Kafka, Elasticsearch local |

## Services

### Discovery Server

Đây là service registry.

Nó giống một danh bạ nội bộ. Khi service khởi động, nó đăng ký tên của mình vào Eureka. Service khác có thể gọi nhau thông qua tên service thay vì hard-code địa chỉ.

Ví dụ:

```text
ORDER-SERVICE muốn gọi INVENTORY-SERVICE
-> Eureka giúp tìm địa chỉ hiện tại của INVENTORY-SERVICE
```

### API Gateway

Gateway là cửa chính của hệ thống.

Client không cần gọi trực tiếp từng service. Client chỉ gọi Gateway, Gateway sẽ chuyển request đến đúng service.

Ví dụ:

```text
Client gọi /product/**
-> Gateway chuyển sang Product Service

Client gọi /order/**
-> Gateway chuyển sang Order Service
```

Gateway cũng là nơi xử lý JWT security trước khi request đi vào service.

### Identity Service

Identity Service xử lý:

- Đăng ký user.
- Đăng nhập.
- Cấp JWT token.
- Xác thực user.

JWT token giống như vé vào hệ thống. Sau khi đăng nhập, client dùng token này để gọi các API cần bảo vệ.

### Profile Service

Profile Service lưu thông tin cá nhân của user.

Ví dụ:

- Tên.
- Email.
- Số điện thoại.
- Địa chỉ.

### Product Service

Product Service quản lý thông tin sản phẩm:

- Tên sản phẩm.
- Mô tả.
- Giá.
- Hình ảnh.
- Category.
- Trạng thái `ACTIVE` hoặc không.
- Seller.

Product Service hiện có field `quantity`, nhưng field này không phải nguồn tồn kho thật. Nó là bản sao được sync từ Inventory Service để list sản phẩm nhanh hơn.

### Inventory Service

Inventory Service là kho hàng thật.

Nó quản lý 3 số lượng quan trọng:

| Field | Ý nghĩa |
| --- | --- |
| `availableQuantity` | Số lượng còn có thể bán |
| `reservedQuantity` | Số lượng đang giữ cho order chưa thanh toán |
| `soldQuantity` | Số lượng đã bán |

Ví dụ:

```text
availableQuantity = 100
reservedQuantity = 0
soldQuantity = 0
```

Khi user tạo order 2 sản phẩm:

```text
availableQuantity = 98
reservedQuantity = 2
soldQuantity = 0
```

Khi payment thành công:

```text
availableQuantity = 98
reservedQuantity = 0
soldQuantity = 2
```

Khi payment thất bại:

```text
availableQuantity = 100
reservedQuantity = 0
soldQuantity = 0
```

### Order Service

Order Service quản lý đơn hàng.

Nó không tự quyết định còn hàng hay không. Khi tạo order, nó gọi Inventory Service để reserve hàng.

Các trạng thái chính:

| Status | Ý nghĩa |
| --- | --- |
| `PENDING` | Order vừa tạo, chưa reserve xong |
| `PENDING_PAYMENT` | Đã giữ hàng, đang chờ thanh toán |
| `CONFIRMED` | Thanh toán thành công, order xác nhận |
| `CANCELLED` | Order bị hủy |
| `INVENTORY_FAILED` | Không reserve được hàng |

### Payment Service

Payment Service quản lý thanh toán.

Payment chỉ được tạo khi order đang ở trạng thái:

```text
PENDING_PAYMENT
```

Điều này nghĩa là hàng đã được giữ trong kho, giờ user mới được thanh toán.

Payment có các trạng thái chính:

| Status | Ý nghĩa |
| --- | --- |
| `PENDING` | Payment mới tạo |
| `SUCCESS` | Thanh toán thành công |
| `FAILED` | Thanh toán thất bại |
| `CANCELLED` | Thanh toán bị hủy |

Service đã chặn các chuyển trạng thái sai, ví dụ:

```text
FAILED -> SUCCESS: không cho
CANCELLED -> SUCCESS: không cho
SUCCESS -> FAILED: không cho
```

### Search Service

Search Service dùng Elasticsearch để tìm sản phẩm nhanh.

Product Service publish event khi product được tạo/sửa/xóa. Search Service nghe các event này để cập nhật Elasticsearch.

Search Service phục vụ các use case:

- Tìm theo tên sản phẩm.
- Lọc theo category.
- Lọc theo giá.
- Lọc còn hàng/hết hàng.
- Sort theo giá hoặc ngày tạo.

### Notification Service

Notification Service nhận event từ Kafka và gửi thông báo/email.

Ví dụ:

- User mới được tạo.
- Order được tạo.
- Order bị hủy.
- Trạng thái order thay đổi.

## REST Và Kafka Là Gì?

Project dùng cả REST và Kafka.

### REST

REST giống như gọi điện trực tiếp và chờ câu trả lời ngay.

Ví dụ:

```text
Order Service hỏi Inventory Service:
"Sản phẩm này còn hàng không? Giữ giúp tôi 2 cái."

Inventory Service trả lời ngay:
"OK, đã giữ hàng."
```

REST phù hợp cho việc cần kết quả ngay lập tức.

Trong project này, REST dùng cho:

- Order gọi Product để lấy thông tin sản phẩm.
- Order gọi Inventory để reserve/release hàng.
- Payment gọi Order để kiểm tra order có được thanh toán không.
- Product detail gọi Inventory để lấy số lượng mới nhất.

### Kafka

Kafka giống như bảng thông báo nội bộ.

Một service đăng tin:

```text
"Payment đã thành công."
```

Các service quan tâm sẽ tự đọc tin đó và xử lý.

Kafka phù hợp cho các việc không cần trả lời ngay cho người gọi.

Trong project này, Kafka dùng cho:

- Payment báo thanh toán thành công/thất bại.
- Inventory báo tồn kho thay đổi.
- Product báo sản phẩm được tạo/sửa/xóa.
- Order báo trạng thái order thay đổi.

## Core Architecture

Mô hình tồn kho hiện tại:

```text
Inventory Service = source of truth
Product Service = giữ quantity denormalized để list nhanh
Search Service = giữ inStock để search/filter nhanh
Product detail / checkout = lấy stock chính xác từ Inventory Service
```

Giải thích:

- Inventory Service là nơi duy nhất quyết định hàng thật còn bao nhiêu.
- Product Service giữ bản sao `quantity` để hiển thị nhanh khi list sản phẩm.
- Search Service giữ `inStock` để filter nhanh khi search.
- Khi vào chi tiết sản phẩm hoặc checkout, hệ thống hỏi trực tiếp Inventory Service để chắc chắn số lượng mới nhất.

Xem flow kỹ hơn tại:

[architecture.md](./architecture.md)

## Main Flows

### 1. Tạo Product Và Inventory

Flow:

```text
Seller tạo product
-> Product Service lưu product
-> Product Service publish product-created
-> Search Service index product

Seller/Admin tạo inventory
-> Inventory Service lưu stock thật
-> Inventory Service publish inventory-updated
-> Product Service sync product.quantity
-> Product Service publish product-updated
-> Search Service update inStock
```

Giải thích:

Khi tạo product, hệ thống chỉ tạo thông tin sản phẩm như tên, giá, mô tả. Sau đó cần tạo inventory riêng để hệ thống biết sản phẩm đó có bao nhiêu hàng.

Khi inventory thay đổi, Inventory Service gửi event `inventory-updated`. Product Service nghe event này để cập nhật lại `quantity`. Sau đó Product Service gửi tiếp `product-updated` để Search Service cập nhật dữ liệu tìm kiếm.

### 2. User Xem Product List

Flow:

```text
Client
-> Gateway
-> Product Service hoặc Search Service
-> Trả danh sách sản phẩm
```

Product list dùng dữ liệu denormalized:

```text
product.quantity
search.inStock
```

Dữ liệu này nhanh để đọc, phù hợp cho trang danh sách. Product Service cũng có batch call tới Inventory Service để lấy `availableQuantity` mới nhất cho nhiều product trong cùng một page, tránh gọi từng product một.

### 3. User Vào Product Detail

Flow:

```text
Client
-> Gateway
-> Product Service
-> Product Service gọi Inventory Service
-> Trả product detail với quantity mới nhất
```

Lý do:

Trang chi tiết cần số lượng chính xác hơn list. Vì vậy Product Service gọi Inventory Service để lấy `availableQuantity` mới nhất.

### 4. User Tạo Order

Flow:

```text
Client tạo order
-> Order Service lấy product name/price/status từ Product Service
-> Order Service save order PENDING
-> Order Service gọi Inventory Service reserve
-> Nếu reserve OK: order -> PENDING_PAYMENT
-> Nếu reserve fail: order -> INVENTORY_FAILED
```

Giải thích:

Order Service chỉ lấy thông tin sản phẩm như tên, giá, trạng thái. Nó không dùng `product.quantity` để quyết định còn hàng.

Inventory Service mới kiểm tra còn hàng thật hay không.

Nếu còn hàng, Inventory Service giữ hàng lại bằng cách:

```text
availableQuantity giảm
reservedQuantity tăng
```

### 5. User Thanh Toán Thành Công

Flow:

```text
Payment Service mark payment SUCCESS
-> publish payment-success
-> Order Service consume event: order -> CONFIRMED
-> Inventory Service consume event: reservation -> CONFIRMED
-> reservedQuantity giảm
-> soldQuantity tăng
-> Inventory Service publish inventory-updated
-> Product/Search sync quantity/inStock
```

Giải thích:

Khi thanh toán thành công, order được xác nhận. Hàng đang giữ trong kho được chuyển thành hàng đã bán.

### 6. Payment Thất Bại Hoặc Bị Hủy

Flow:

```text
Payment Service mark FAILED/CANCELLED
-> publish payment-failed/payment-cancelled
-> Inventory Service release reservation
-> reservedQuantity giảm
-> availableQuantity tăng lại
-> Inventory Service publish inventory-updated
-> Product/Search sync quantity/inStock
```

Giải thích:

Nếu user không thanh toán thành công, hàng đang giữ phải được trả lại kho để người khác có thể mua.

### 7. User Hủy Order

Flow:

```text
Client gọi cancel order
-> Nếu order đang PENDING_PAYMENT, Order Service gọi Inventory Service release
-> Order status -> CANCELLED
-> Order Service publish order-cancelled
```

Giải thích:

Nếu order đã giữ hàng, khi hủy phải release hàng.

## API Gateway Routes

Gateway chạy ở:

```text
http://localhost:9191
```

Các route chính:

| Gateway path | Target service |
| --- | --- |
| `/identity/**` | Identity Service |
| `/profile/**` | Profile Service |
| `/product/**` | Product Service |
| `/search/**` | Search Service |
| `/api/v1/search/**` | Search Service |
| `/inventory/**` | Inventory Service |
| `/api/v1/inventory/**` | Inventory Service |
| `/order/**` | Order Service |
| `/payment/**` | Payment Service |
| `/notification/**` | Notification Service |

## Local Setup

### 1. Start Infrastructure

Chạy các dependency bằng Docker Compose:

```bash
docker compose up -d
```

Docker Compose hiện gồm:

- PostgreSQL
- MongoDB
- Redis
- Kafka
- Kafka UI
- Elasticsearch
- Kibana

Useful URLs:

```text
Kafka UI: http://localhost:8085
Elasticsearch: http://localhost:9200
Kibana: http://localhost:5601
Eureka: http://localhost:8761
API Gateway: http://localhost:9191
```

### Seed demo catalog and inventory

The catalog and inventory are separate databases. Run the product seed first,
then run the inventory seed so every seeded product has stock available for
checkout, including the product variants.

```bash
docker compose exec -T product-postgres psql -U root -d postgres < database/seed-products.sql
docker compose exec -T inventory-postgres psql -U postgres -d inventory_db < database/seed-inventory.sql
```

`seed-inventory.sql` is idempotent: it inserts only missing inventory rows and
does not overwrite quantities that have already changed through an order flow.
For an existing database created by an older version of the product seed, use a
fresh demo database before reseeding because the older sample-product IDs were
random rather than deterministic.

### 2. Start Services

Nên chạy theo thứ tự:

```bash
cd discovery-server
./mvnw spring-boot:run
```

```bash
cd api-gateway-service
./mvnw spring-boot:run
```

Sau đó chạy các business service:

```bash
cd product-service
./mvnw spring-boot:run
```

```bash
cd inventory-service
./mvnw spring-boot:run
```

```bash
cd order-service
./mvnw spring-boot:run
```

```bash
cd payment-service
./mvnw spring-boot:run
```

```bash
cd search-service
./mvnw spring-boot:run
```

```bash
cd notification-service
./mvnw spring-boot:run
```

## Service Ports

Các service chính hiện đã được cấu hình port không trùng nhau:

```text
identity-service: 8080
profile-service: 8081
notification-service: 8083
product-service: 8084
order-service: 8086
inventory-service: 8087
payment-service: 8088
search-service: 8089
discovery-server: 8761
api-gateway-service: 9191
```

## Build Check

Compile từng service:

```bash
cd product-service
./mvnw -DskipTests compile
```

```bash
cd inventory-service
./mvnw -DskipTests compile
```

```bash
cd order-service
./mvnw -DskipTests compile
```

```bash
cd payment-service
./mvnw -DskipTests compile
```

```bash
cd search-service
./mvnw -DskipTests compile
```

## Suggested Demo Flow

Đây là flow nên demo khi giới thiệu project:

1. Start infrastructure bằng Docker Compose.
2. Start Eureka, Gateway và các service chính.
3. Register/login để lấy JWT.
4. Tạo category.
5. Tạo product.
6. Tạo inventory cho product.
7. Search/list product.
8. Vào product detail để thấy quantity mới nhất.
9. Tạo order.
10. Kiểm tra order status `PENDING_PAYMENT`.
11. Tạo payment.
12. Mark payment success.
13. Kiểm tra order chuyển `CONFIRMED`.
14. Kiểm tra inventory `reservedQuantity` giảm và `soldQuantity` tăng.
15. Kiểm tra product/search sync lại stock.

## Những Câu Hỏi Phỏng Vấn Dễ Gặp

### Vì sao tách Inventory Service riêng?

Vì tồn kho là nghiệp vụ quan trọng và dễ bị sai nếu nhiều service cùng tự trừ số lượng.

Project chọn:

```text
Inventory Service = source of truth
```

Nhờ vậy, mọi quyết định còn hàng/hết hàng đều tập trung ở một nơi.

### Vì sao Product Service vẫn có quantity?

Vì Product Service cần list sản phẩm nhanh. Nếu mỗi lần list 100 sản phẩm đều gọi Inventory Service 100 lần thì chậm.

Vì vậy `product.quantity` là bản sao denormalized:

```text
Nhanh để đọc
Không phải nguồn quyết định stock thật
Được sync từ Inventory Service qua Kafka
```

### Vì sao dùng Kafka?

Kafka giúp service giao tiếp bất đồng bộ.

Ví dụ khi payment thành công:

```text
Payment Service chỉ cần publish payment-success
Order Service tự nghe để confirm order
Inventory Service tự nghe để confirm inventory
```

Payment Service không cần gọi trực tiếp từng service một.

### Vì sao vẫn dùng REST?

REST dùng khi cần câu trả lời ngay.

Ví dụ tạo order cần biết reserve inventory có thành công không. Vì vậy Order Service gọi Inventory Service trực tiếp bằng REST.

### Project có bị loop service không?

Không có loop nguy hiểm.

Flow chính hiện tại là:

```text
Order -> Product
Order -> Inventory
Payment -> Kafka
Kafka -> Order/Inventory
Inventory -> Kafka
Kafka -> Product/Search
```

Product Service không còn nghe `order-created` để trừ stock nữa, nên không có chuyện Product và Inventory cùng trừ hàng.

## Future Improvements

- Tách event DTO ra module chung như `common-event`.
- Thêm Outbox Pattern cho event quan trọng.
- Thêm retry + Dead Letter Topic cho Kafka consumers.
- Đổi tên `product.quantity` thành `cachedAvailableQuantity`.
- Thêm Flyway/Liquibase thay cho `ddl-auto: update`.
- Thêm tracing/log correlation cho flow order-payment-inventory.
