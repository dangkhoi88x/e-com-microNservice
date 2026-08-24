# ELK logging trong project e-com-microNservice

Tài liệu này giải thích từ nền tảng về ELK, cách log đi qua hệ thống, và cách cấu hình hiện tại hoạt động với `payment-service` trong project này.

## 1. Vấn đề: log bị phân tán trong microservice

Một ứng dụng monolith thường chỉ có một process. Khi xảy ra lỗi, có thể mở console hoặc file log của process đó để kiểm tra.

Microservice khác hơn. Một thao tác của người dùng có thể đi qua nhiều service:

```text
Web app -> API Gateway -> order-service -> payment-service -> inventory-service
```

Ví dụ người dùng đặt hàng nhưng thanh toán thất bại. Log có thể nằm ở gateway, order service, payment service và inventory service. Việc vào từng container hoặc từng terminal để tìm log rất chậm, khó đối chiếu thời gian và khó tổng hợp lỗi.

**Centralized logging** (logging tập trung) giải quyết vấn đề này: mọi service gửi log về một hệ thống chung. ELK là một bộ công cụ phổ biến để làm việc đó.

## 2. ELK là gì?

ELK là tên ghép của ba thành phần:

| Thành phần | Vai trò |
|---|---|
| Elasticsearch | Lưu trữ, lập chỉ mục và tìm kiếm log nhanh |
| Logstash | Nhận log, parse/biến đổi/gắn thêm dữ liệu rồi gửi tiếp |
| Kibana | Giao diện web để tìm, lọc, trực quan hóa log |

Trong project này còn có một thành phần ở phía Spring Boot: **Logback**. Logback là logging framework mà Spring Boot sử dụng mặc định. Nó tạo log event và được cấu hình để gửi event đó đến Logstash.

## 3. Kiến trúc logging của project

Khi chạy `payment-service` từ IntelliJ, luồng hiện tại là:

```text
payment-service (Spring Boot, IntelliJ)
  -> Logback tạo JSON
  -> localhost:5600
  -> Logstash Docker container
  -> Elasticsearch Docker container
  -> Kibana trên trình duyệt
```

Chi tiết hơn:

```text
log.info("Payment completed")
      |
      v
logback-spring.xml
      |
      | TCP, JSON Lines, port 5600
      v
Logstash input/filter/output
      |
      v
Elasticsearch index: elk-bookstore-YYYY.MM.dd
      |
      v
Kibana data view: elk-bookstore-*
```

Các port liên quan:

| Port | Thành phần | Mục đích |
|---:|---|---|
| 5600 | Logstash | Nhận JSON log qua TCP |
| 9200 | Elasticsearch | REST API để kiểm tra index/search |
| 5601 | Kibana | Giao diện web |
| 5432 | PostgreSQL | Database mà payment-service hiện đang kết nối |
| 8088 | payment-service | API của payment service |
| 8761 | Discovery Server/Eureka | Đăng ký và tìm service |

## 4. Structured logging: tại sao log JSON tốt hơn text?

Log text truyền thống thường như sau:

```text
2026-08-05 11:41:43 INFO PaymentService - Payment created successfully
```

Con người đọc được, nhưng công cụ phải phân tích chuỗi ký tự để biết thời gian, mức log và service nào tạo log.

Với structured logging, một log event là JSON có các field rõ ràng:

```json
{
  "@timestamp": "2026-08-05T04:41:43.583Z",
  "appName": "book-store",
  "serviceName": "payment-service",
  "level": "INFO",
  "logger_name": "com.example.paymentservice.PaymentServiceApplication",
  "message": "Started PaymentServiceApplication in 6.585 seconds"
}
```

Kibana và Elasticsearch hiểu từng field. Vì vậy có thể tìm chính xác:

```text
serviceName : "payment-service"
```

Hoặc chỉ xem lỗi:

```text
level : "ERROR"
```

Không cần dò từng dòng text bằng mắt.

## 5. Các file đã thêm trong project

### 5.1 `payment-service/pom.xml`

`payment-service/pom.xml` có dependency:

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>8.1</version>
</dependency>
```

Dependency này cung cấp `LogstashTcpSocketAppender` và JSON encoder. Nếu không có nó, Logback chỉ có các appender cơ bản như console/file; không biết cách gửi JSON event qua TCP đến Logstash.

### 5.2 `payment-service/src/main/resources/logback-spring.xml`

Đây là cấu hình Logback của `payment-service`.

Hai include đầu nạp cấu hình console chuẩn của Spring Boot:

```xml
<include resource="org/springframework/boot/logging/logback/defaults.xml"/>
<include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
```

Vì vậy log vẫn hiện trong Console của IntelliJ. Đây là điều hữu ích khi phát triển local.

Phần sau lấy tên service từ Spring configuration:

```xml
<springProperty name="serviceName"
                source="spring.application.name"
                defaultValue="payment-service"/>
```

Trong `application.yaml`, giá trị tương ứng là:

```yaml
spring:
  application:
    name: payment-service
```

Vì thế mỗi event được gắn `serviceName: payment-service`. Khi áp dụng cùng cấu hình cho service khác, chỉ cần mỗi service đặt đúng `spring.application.name`.

Appender chính là:

```xml
<appender name="LOGSTASH"
          class="net.logstash.logback.appender.LogstashTcpSocketAppender">
```

Nó mở TCP connection đến destination:

```xml
<destination>${LOGSTASH_DESTINATION:-localhost:5600}</destination>
```

Ý nghĩa cú pháp `${A:-B}`:

- Nếu có biến môi trường `LOGSTASH_DESTINATION`, dùng giá trị đó.
- Nếu không có, dùng mặc định `localhost:5600`.

Bạn đang chạy từ IntelliJ trên Windows, nên `localhost:5600` là đúng: Docker publish port `5600` từ Logstash container ra máy host.

Encoder trong file tạo các field JSON như thời gian, level, logger, thread, message, stack trace, MDC và metadata cố định:

```json
{
  "appName": "book-store",
  "serviceName": "payment-service"
}
```

Cuối file root logger gửi cùng một log event đến cả Console lẫn Logstash:

```xml
<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="LOGSTASH"/>
</root>
```

Vì level là `INFO`, những event `INFO`, `WARN`, `ERROR` được gửi. `DEBUG` và `TRACE` không được gửi để tránh lượng log quá lớn trong môi trường thông thường.

## 6. Logstash pipeline

File pipeline là `elk/logstash/pipeline/logstash.conf`.

### 6.1 Input

```conf
input {
  tcp {
    port => 5600
    codec => json_lines
  }
}
```

Logstash mở TCP port `5600`. `json_lines` nói rằng mỗi dòng nhận được là một JSON document. Logback gửi event theo đúng format này nên Logstash tách được các field như `message`, `level`, `serviceName`, thay vì coi toàn bộ event là một chuỗi text.

### 6.2 Filter

```conf
filter {
  mutate {
    lowercase => ["appName", "serviceName"]
    remove_field => ["host", "@version"]
  }

  if [level] == "ERROR" {
    mutate {
      add_tag => ["ERROR_LOG"]
    }
  }
}
```

`mutate` là filter dùng để sửa event:

- `lowercase`: chuẩn hóa tên ứng dụng/service thành chữ thường. Đây là cách tốt để query không bị lệch do `Payment-Service` và `payment-service`.
- `remove_field`: xóa metadata không cần lưu.
- Event có `level = ERROR` được thêm tag `ERROR_LOG`. Sau này Kibana có thể lọc `tags : "ERROR_LOG"`.

### 6.3 Output

```conf
output {
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
    index => "elk-bookstore-%{+YYYY.MM.dd}"
  }

  stdout {
    codec => rubydebug
  }
}
```

Trong Docker Compose, hostname `elasticsearch` là tên service Elasticsearch nên Logstash gọi được qua Docker network.

Tên index chứa ngày, ví dụ `elk-bookstore-2026.08.05`. Chia index theo ngày giúp quản lý retention và xóa log cũ sau này đơn giản hơn.

`stdout` với `rubydebug` in event đã xử lý ra terminal Logstash. Trong lúc học, đây là cách nhanh nhất để biết Logstash có nhận được log hay chưa.

## 7. Docker Compose của ELK

`docker-compose.yaml` có ba service chính:

```text
elasticsearch
logstash
kibana
```

Logstash mount thư mục pipeline của project:

```yaml
volumes:
  - ./elk/logstash/pipeline:/usr/share/logstash/pipeline:ro
```

`ro` nghĩa là read-only: container chỉ đọc pipeline configuration, không ghi đè file cấu hình trong source code.

Elasticsearch chạy single-node và security đang tắt:

```yaml
discovery.type: single-node
xpack.security.enabled: false
```

Điều này phù hợp để học local. Không nên giữ cấu hình này trong production public, vì Elasticsearch khi đó có thể bị truy cập không cần xác thực.

## 8. Cách khởi động khi chạy service từ IntelliJ

Từ thư mục gốc project:

```powershell
docker compose up -d elasticsearch logstash kibana product-postgres
```

Giải thích:

- `elasticsearch`, `logstash`, `kibana`: chạy hệ thống ELK.
- `product-postgres`: payment service hiện cấu hình database `jdbc:postgresql://localhost:5432/postgres`, trùng với PostgreSQL container đang expose cổng `5432`.

Sau đó chạy `PaymentServiceApplication` trong IntelliJ.

Nếu service báo:

```text
Connection to localhost:5432 refused
```

thì PostgreSQL chưa chạy hoặc port `5432` đang không mở. Kiểm tra bằng:

```powershell
docker compose ps product-postgres
Test-NetConnection localhost -Port 5432
```

Nếu Eureka/Discovery Server chưa chạy, payment service có thể ghi các warning không kết nối được `localhost:8761`. Đó là lỗi discovery độc lập với ELK. Service vẫn có thể khởi động; để hết cảnh báo, chạy `discovery-server` từ IntelliJ hoặc Docker.

## 9. Cách xác minh hệ thống hoạt động

### Kiểm tra Logstash

```powershell
docker compose logs -f logstash
```

Nếu thấy event chứa `serviceName => "payment-service"`, Logback đã gửi log thành công đến Logstash.

### Kiểm tra Elasticsearch

```powershell
Invoke-RestMethod http://localhost:9200/_cat/indices?v
```

Kết quả cần có index giống:

```text
elk-bookstore-2026.08.05
```

`yellow` với Elasticsearch một node là bình thường: primary shard hoạt động nhưng replica shard không có node thứ hai để đặt. Không phải lỗi mất dữ liệu trong môi trường học local.

### Kiểm tra trong Kibana

Mở `http://localhost:5601`, vào **Discover**, rồi tạo Data View:

```text
Name: Bookstore logs
Index pattern: elk-bookstore-*
Timestamp field: @timestamp
```

Sau đó chọn data view `Bookstore logs`. Các field như `appName`, `serviceName`, `level`, `logger_name`, `message`, `stack_trace` sẽ xuất hiện ở thanh bên trái.

## 10. Các truy vấn Kibana KQL hữu ích

Chỉ xem payment service:

```text
serviceName : "payment-service"
```

Chỉ xem error:

```text
level : "ERROR"
```

Xem warning hoặc error:

```text
level : "WARN" or level : "ERROR"
```

Tìm theo nội dung message:

```text
message : "*Eureka*"
```

Lọc theo application:

```text
appName : "book-store"
```

## 11. MDC và trace ID: bước tiếp theo khi có nhiều service

`<mdc/>` trong `logback-spring.xml` cho phép Logback gửi thêm dữ liệu trong MDC (Mapped Diagnostic Context).

Ví dụ tốt nhất trong microservice là `traceId` hoặc `requestId`. Gateway tạo một trace ID cho request, truyền nó qua HTTP headers/Kafka headers, và mọi service đặt nó vào MDC. Mọi log của cùng request sẽ có chung giá trị:

```json
{
  "traceId": "c8a5d6f1",
  "serviceName": "order-service",
  "message": "Order created"
}
```

```json
{
  "traceId": "c8a5d6f1",
  "serviceName": "payment-service",
  "message": "Payment completed"
}
```

Kibana query:

```text
traceId : "c8a5d6f1"
```

Kết quả là toàn bộ hành trình của một request xuyên qua nhiều microservice. Đây là giá trị lớn nhất của centralized logging trong hệ thống microservice.

### Trace ID qua Kafka

MDC là dữ liệu theo thread nên không tự đi qua Kafka. Khi một service publish event, Kafka producer interceptor lấy `traceId` từ MDC và thêm nó vào Kafka header `X-Trace-Id`. Consumer record interceptor của service nhận event đọc header đó, đặt lại vào MDC trong thời gian xử lý listener và xóa MDC sau khi xử lý xong.

Trong project hiện tại, `order-service`, `payment-service` và `inventory-service` đã có producer interceptor. `order-service` và `inventory-service` cũng có consumer interceptor. Vì vậy các event như `payment-success`, `payment-failed` và `payment-cancelled` giữ cùng trace ID khi đi từ payment service sang order/inventory.

`order-created` cũng được publish kèm header trace ID. Consumer thực tế của topic này là `notification-service`, nơi đã được cấu hình ELK và Kafka consumer interceptor. Vì vậy một query theo trace ID có thể nối log `Order created` của order service với `Received OrderCreatedEvent` của notification service.

## 12. Khi áp dụng sang các service khác

Sau khi `payment-service` ổn định, lặp lại cho `order-service`, `product-service`, `inventory-service` và các service khác:

1. Thêm dependency `logstash-logback-encoder`.
2. Thêm `logback-spring.xml` tương tự.
3. Bảo đảm `spring.application.name` đúng với service đó.
4. Chạy service và kiểm tra `serviceName` trên Kibana.

Không nên copy cứng `serviceName` trong XML. Cấu hình hiện tại lấy nó từ `spring.application.name`, nên cùng một template có thể tái sử dụng.

## 13. Tóm tắt

ELK hiện hoạt động theo nguyên lý:

```text
Spring Boot log event
  -> Logback JSON encoder
  -> Logstash nhận và biến đổi event
  -> Elasticsearch lưu/index event
  -> Kibana tìm kiếm và quan sát event
```

Trong môi trường local hiện tại, payment service chạy ngoài Docker từ IntelliJ nên gửi log đến `localhost:5600`. Logstash, Elasticsearch và Kibana chạy bằng Docker Compose. Kibana đọc index `elk-bookstore-*` để hiển thị toàn bộ log có cấu trúc của ứng dụng.

## 14. Trace ID trong HTTP và Kafka

### 14.1 Trace ID qua HTTP

Project dùng header `X-Trace-Id` để liên kết log của cùng một request.

```text
Client
  -> api-gateway-service tạo hoặc giữ X-Trace-Id
  -> order-service đọc header và đặt traceId vào MDC
  -> HTTP client của order-service forward header đến product/inventory/promotion/cart/shipping
  -> service đích đọc header và đưa traceId vào MDC
```

`api-gateway-service` có `TraceIdGatewayFilter`. Filter này tạo UUID nếu client chưa gửi `X-Trace-Id`, forward header sang service phía sau và trả header đó về response.

`order-service`, `product-service`, `inventory-service`, `payment-service` và `notification-service` có `TraceIdFilter`. Với mỗi HTTP request, filter:

1. Đọc `X-Trace-Id`, hoặc tạo mới nếu header không có.
2. Đặt giá trị vào `MDC` với key `traceId`.
3. Chạy request bình thường.
4. Xóa MDC trong `finally` để thread server tái sử dụng không mang trace ID cũ.

Vì `logback-spring.xml` có `<mdc/>`, mọi log trong request sẽ có field JSON `traceId`.

### 14.2 Trace ID qua Kafka

MDC không tự đi qua Kafka vì Kafka producer và consumer không cùng HTTP request/thread. Project dùng hai interceptor:

```text
Kafka producer interceptor
  -> đọc traceId từ MDC
  -> thêm Kafka header X-Trace-Id

Kafka consumer record interceptor
  -> đọc Kafka header X-Trace-Id
  -> đặt traceId vào MDC trước khi listener chạy
  -> xóa MDC sau khi listener hoàn tất
```

Interceptor này đã được áp dụng cho `order-service`, `payment-service`, `inventory-service` và `notification-service` theo vai trò producer/consumer của từng service.

Ví dụ luồng tạo đơn:

```text
Gateway request
  -> order-service: "Order created"
  -> Kafka topic order-created (X-Trace-Id header)
  -> notification-service: "Received OrderCreatedEvent"
```

Trong Kibana, copy `traceId` ở một document rồi query:

```text
traceId : "gia-tri-trace-id"
```

Kết quả phải hiển thị log của cả `order-service` và `notification-service` có cùng trace ID.

Lưu ý: `order-created` hiện được `notification-service` tiêu thụ. Các event `payment-success`, `payment-failed` và `payment-cancelled` từ `payment-service` được tiêu thụ bởi `order-service` và `inventory-service`.

## 15. Dashboard Kibana đơn giản

Dashboard hiện dùng các field có sẵn (`serviceName.keyword`, `level.keyword`, `message.keyword`) để học nhanh. Cách này phù hợp local; về lâu dài nên thêm các field nghiệp vụ riêng như `eventType`, `outcome` và `exceptionType`.

### Chuẩn bị

1. Mở Kibana tại `http://localhost:5601`.
2. Vào **Analytics -> Dashboard -> Create dashboard**.
3. Bấm **Create visualization**. Màn hình editor đang hiện là Lens.
4. Chọn data view `Bookstore logs` và chỉnh time range, ví dụ **Last 24 hours**.

### Panel 1: Total logs by service

Tạo Bar chart:

1. Kéo `serviceName.keyword` vào **Horizontal axis**.
2. Kéo `Records` vào **Vertical axis** để có `Count of records`.
3. Lưu với tên `Total logs by service`.

### Panel 2: Errors by service

Tạo Bar chart:

1. Thêm KQL filter:

```text
level.keyword : "ERROR"
```

2. Kéo `serviceName.keyword` vào **Horizontal axis**.
3. Kéo `Records` vào **Vertical axis**.
4. Lưu với tên `Errors by service`.

### Panel 3: Orders created

Tạo Metric:

1. Thêm KQL filter:

```text
serviceName.keyword : "order-service" and message.keyword : "Order created*"
```

2. Đổi visualization type sang **Metric**.
3. Kéo `Records` vào **Primary metric**. Không kéo `message.keyword` vào metric vì Lens sẽ tính unique count của message thay vì đếm log.
4. Lưu với tên `Orders created`.

Metric hiện `N/A` nghĩa là không có document khớp query, không phải lỗi Lens. Cần restart `order-service`, tạo một đơn thành công rồi kiểm tra trong Discover có message `Order created: orderId=...`.

Nếu chỉ muốn kiểm tra Metric có hoạt động, dùng filter tạm:

```text
serviceName.keyword : "order-service"
```

Khi đó metric đếm toàn bộ log order service, không phải số đơn.

### Panel 4: Payment failures over time

Tạo Line chart hoặc Bar chart:

1. Thêm KQL filter:

```text
serviceName.keyword : "payment-service" and message.keyword : "Published PaymentFailedEvent*"
```

2. Kéo `@timestamp` vào **Horizontal axis** và chọn **Date histogram**.
3. Kéo `Records` vào **Vertical axis**.
4. Lưu với tên `Payment failures over time`.

Nếu không có payment failure event trong time range, panel trống là bình thường.

### Lưu dashboard

Sau khi có bốn panel, sắp xếp chúng rồi bấm **Save**. Tên đề xuất:

```text
Bookstore - Logging Overview
```

Có thể thêm panel `Recent errors` sau này bằng Data table, filter `level.keyword : "ERROR"`, các cột `@timestamp`, `serviceName.keyword`, `logger_name.keyword`, `message.keyword`, rồi sort thời gian giảm dần.
