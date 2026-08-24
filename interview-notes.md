# Interview Notes - Khoi Microservice E-commerce

Tài liệu này dùng để ôn phỏng vấn và luyện cách giải thích project `Khoi-Micro`.

Mục tiêu:

- Giải thích project rõ ràng, dễ hiểu.
- Trả lời được các câu hỏi thường gặp về microservices.
- Biết điểm mạnh, điểm yếu, trade-off của project.
- Trình bày được flow order-payment-inventory một cách mạch lạc.

## 1. Giới Thiệu Project Ngắn Gọn

Nếu interviewer hỏi:

```text
Em giới thiệu project này đi.
```

Có thể trả lời:

```text
Đây là project e-commerce backend em xây dựng theo kiến trúc microservices bằng Spring Boot.

Project gồm các service chính như Identity, Product, Search, Inventory, Order, Payment, Notification, API Gateway và Discovery Server.

Em dùng REST cho các thao tác cần phản hồi ngay, ví dụ Order gọi Inventory để reserve hàng. Em dùng Kafka cho các sự kiện bất đồng bộ, ví dụ Payment thành công thì publish event để Order confirm đơn và Inventory confirm tồn kho.

Điểm em tập trung nhiều nhất là flow order-payment-inventory, đặc biệt là tách Inventory Service làm source of truth cho tồn kho.
```

Phiên bản ngắn hơn:

```text
Project của em là hệ thống e-commerce microservices. Em tách các nghiệp vụ như product, inventory, order, payment, search thành các service riêng. REST dùng cho luồng cần kết quả ngay, Kafka dùng cho event bất đồng bộ. Inventory Service là nơi quyết định tồn kho thật.
```

## 2. Vì Sao Chọn Microservices?

Câu hỏi:

```text
Vì sao em không làm monolith mà lại làm microservices?
```

Trả lời:

```text
Vì em muốn mô phỏng cách một hệ thống e-commerce thực tế có thể được chia nhỏ theo domain.

Ví dụ Product Service chỉ quản lý thông tin sản phẩm, Inventory Service quản lý tồn kho, Order Service quản lý đơn hàng, Payment Service quản lý thanh toán.

Khi tách như vậy, mỗi service có responsibility rõ hơn, dễ mở rộng từng phần, và dễ thay đổi một nghiệp vụ mà không ảnh hưởng quá nhiều đến phần khác.
```

Nên nói thêm trade-off:

```text
Tuy nhiên microservices cũng phức tạp hơn monolith. Nó cần xử lý service discovery, gateway, network failure, event consistency, retry, monitoring. Vì vậy với project nhỏ thực tế có thể bắt đầu bằng monolith trước, còn project này em làm microservices để học và demo kiến trúc phân tán.
```

## 3. Tổng Quan Các Service

Khi cần kể nhanh:

| Service | Vai trò |
| --- | --- |
| API Gateway | Cổng vào hệ thống, route request |
| Discovery Server | Eureka registry cho service discovery |
| Identity Service | Đăng ký, đăng nhập, JWT |
| Profile Service | Quản lý profile user |
| Product Service | Quản lý product, category, price, status |
| Search Service | Tìm kiếm product bằng Elasticsearch |
| Inventory Service | Quản lý tồn kho thật |
| Order Service | Tạo order, reserve/release inventory |
| Payment Service | Quản lý payment, publish payment event |
| Notification Service | Consume event và gửi thông báo |

## 4. Flow Quan Trọng Nhất: Order - Payment - Inventory

Đây là flow nên kể kỹ nhất khi phỏng vấn.

### 4.1. Tạo Order Thành Công

```text
1. User tạo order.
2. Order Service gọi Product Service để lấy name, price, status của sản phẩm.
3. Order Service lưu order với status PENDING.
4. Order Service gọi Inventory Service để reserve hàng.
5. Nếu Inventory reserve thành công:
   - availableQuantity giảm.
   - reservedQuantity tăng.
   - order chuyển sang PENDING_PAYMENT.
6. User có thể tạo payment.
```

Câu trả lời gọn:

```text
Order Service không tự trừ stock. Nó chỉ tạo order và gọi Inventory Service reserve. Nếu reserve thành công thì order chuyển sang PENDING_PAYMENT.
```

### 4.2. Payment Thành Công

```text
1. Payment Service chuyển payment từ PENDING sang SUCCESS.
2. Payment Service publish payment-success event.
3. Order Service nghe event này và chuyển order sang CONFIRMED.
4. Inventory Service cũng nghe event này và confirm reservation.
5. Inventory chuyển hàng từ reservedQuantity sang soldQuantity.
6. Inventory publish inventory-updated.
7. Product/Search sync lại quantity/inStock.
```

Câu trả lời gọn:

```text
Payment Service không gọi trực tiếp Order và Inventory để update. Nó publish event payment-success, các service liên quan tự consume và xử lý phần của mình.
```

### 4.3. Payment Failed Hoặc Cancelled

```text
1. Payment chuyển FAILED hoặc CANCELLED.
2. Payment Service publish payment-failed hoặc payment-cancelled.
3. Inventory Service consume event.
4. Inventory release reservation.
5. reservedQuantity giảm.
6. availableQuantity tăng lại.
7. Inventory publish inventory-updated để Product/Search sync lại.
```

Câu trả lời gọn:

```text
Nếu payment fail hoặc cancel, Inventory Service release hàng đã giữ để người khác có thể mua.
```

## 5. Vì Sao Inventory Service Là Source Of Truth?

Câu hỏi:

```text
Stock thật nằm ở đâu?
```

Trả lời:

```text
Stock thật nằm ở Inventory Service.

Product Service chỉ giữ thông tin catalog như tên, giá, mô tả, hình ảnh, status. Product Service có field quantity nhưng đó là bản sao denormalized để list/search nhanh, không phải nguồn quyết định tồn kho.

Khi tạo order, Order Service gọi Inventory Service để reserve hàng. Inventory Service mới là nơi kiểm tra availableQuantity có đủ hay không.
```

Nên nhấn mạnh:

```text
Em chọn cách này để tránh việc Product Service và Inventory Service cùng tự trừ stock dẫn đến lệch dữ liệu.
```

## 6. Vì Sao Product Service Vẫn Có Quantity?

Câu hỏi:

```text
Nếu Inventory là source of truth, sao Product vẫn có quantity?
```

Trả lời:

```text
Product.quantity là dữ liệu denormalized.

Nó giúp list product và filter còn hàng/hết hàng nhanh hơn, tránh việc mỗi lần list nhiều sản phẩm phải gọi Inventory Service nhiều lần.

Khi Inventory thay đổi stock, Inventory Service publish inventory-updated. Product Service consume event đó để sync product.quantity = availableQuantity.
```

Nói thêm trade-off:

```text
Trade-off là quantity bên Product có thể lệch rất ngắn trong thời gian event chưa được consume. Nhưng trước checkout hoặc product detail, hệ thống vẫn gọi Inventory Service để lấy số lượng chính xác hơn.
```

## 7. Product Detail Khác Product List Như Thế Nào?

Câu hỏi:

```text
Product list và product detail lấy stock như nhau không?
```

Trả lời:

```text
Không hoàn toàn giống.

Product list hoặc search dùng quantity/inStock denormalized để đọc nhanh.

Product detail gọi trực tiếp Inventory Service để lấy availableQuantity mới nhất. Như vậy list vẫn nhanh, nhưng khi user xem chi tiết hoặc chuẩn bị checkout thì dữ liệu stock chính xác hơn.
```

## 8. REST Và Kafka Được Dùng Ở Đâu?

### REST

Dùng khi service cần câu trả lời ngay.

Ví dụ:

```text
Order -> Product: lấy product detail.
Order -> Inventory: reserve/release hàng.
Payment -> Order: kiểm tra order có payable không.
Product -> Inventory: lấy quantity mới nhất cho product detail.
```

Câu trả lời:

```text
Em dùng REST cho các bước cần quyết định ngay trong request hiện tại. Ví dụ tạo order phải biết reserve inventory thành công hay thất bại ngay.
```

### Kafka

Dùng khi chỉ cần báo sự kiện để service khác xử lý sau.

Ví dụ:

```text
Payment Service publish payment-success.
Order Service consume để confirm order.
Inventory Service consume để confirm inventory.
```

Câu trả lời:

```text
Em dùng Kafka cho event bất đồng bộ để giảm coupling. Payment Service không cần biết cụ thể có bao nhiêu service cần xử lý payment-success.
```

## 9. Có Bị Loop Service Không?

Câu hỏi:

```text
Flow này có bị vòng lặp giữa các service không?
```

Trả lời:

```text
Không có loop nguy hiểm.

Flow chính là:
Order gọi Product và Inventory bằng REST.
Payment publish event.
Order và Inventory consume payment event.
Inventory publish inventory-updated.
Product consume inventory-updated để sync quantity.
Product publish product-updated cho Search.

Product Service không còn consume order-created để trừ stock, nên không có chuyện Product và Inventory cùng trừ hàng.
```

Sơ đồ nói nhanh:

```text
Order -> Inventory
Payment -> Kafka
Kafka -> Order/Inventory
Inventory -> Kafka
Kafka -> Product/Search
```

## 10. Race Condition Trong Inventory Được Xử Lý Thế Nào?

Câu hỏi:

```text
Nếu 2 user cùng mua sản phẩm còn 1 cái thì sao?
```

Trả lời:

```text
Inventory Service dùng pessimistic lock khi tìm inventory theo productId để reserve.

Khi một transaction đang trừ availableQuantity của product đó, transaction khác phải chờ. Như vậy tránh trường hợp cả hai request cùng đọc availableQuantity cũ rồi cùng trừ sai.
```

Ví dụ:

```text
availableQuantity = 1

User A reserve 1 cái -> lock row -> availableQuantity còn 0
User B chờ lock -> đọc lại thấy 0 -> reserve fail
```

## 11. Idempotency Trong Confirm/Release Inventory

Câu hỏi:

```text
Nếu payment-success event bị gửi 2 lần thì có bị cộng soldQuantity 2 lần không?
```

Trả lời:

```text
Inventory confirm/release chỉ xử lý reservation đang ở trạng thái PENDING.

Nếu reservation đã CONFIRMED hoặc RELEASED rồi, service return và không cộng/trừ lại.

Điều này giúp confirm/release idempotent ở mức demo.
```

## 12. Payment Status Được Bảo Vệ Thế Nào?

Câu hỏi:

```text
Payment failed rồi có chuyển success được không?
```

Trả lời:

```text
Không.

Payment chỉ cho chuyển từ PENDING sang SUCCESS, FAILED hoặc CANCELLED.

Các trạng thái như FAILED, CANCELLED, SUCCESS là trạng thái cuối. Không cho FAILED -> SUCCESS, CANCELLED -> SUCCESS hoặc SUCCESS -> FAILED.
```

## 13. Vì Sao Order Reserve Fail Nhưng Vẫn Lưu Order?

Câu hỏi:

```text
Nếu reserve inventory fail thì sao?
```

Trả lời:

```text
Order Service vẫn lưu order với status INVENTORY_FAILED.

Ở mức demo, em chọn cách không throw exception sau khi set INVENTORY_FAILED để transaction không rollback mất order failed.

Cách này giúp dễ debug và client biết order đã tạo nhưng không reserve được hàng.
```

Nói thêm:

```text
Trong production có thể dùng transaction boundary rõ hơn, outbox hoặc saga để xử lý chặt hơn.
```

## 14. Vì Sao Search Service Không Gọi Product Mỗi Lần Search?

Câu hỏi:

```text
Sao Search Service lại lưu bản sao product?
```

Trả lời:

```text
Search cần tốc độ cao và khả năng filter/sort tốt. Vì vậy Search Service lưu product document trong Elasticsearch.

Product Service publish product-created/product-updated/product-deleted. Search Service consume các event đó để cập nhật index.

Đây là mô hình read model denormalized.
```

Trade-off:

```text
Dữ liệu search có thể eventual consistency, tức là có thể trễ một chút so với Product DB. Nhưng đổi lại search nhanh và tách biệt khỏi database chính.
```

## 15. API Gateway Có Vai Trò Gì?

Câu hỏi:

```text
API Gateway để làm gì?
```

Trả lời:

```text
API Gateway là entry point cho client.

Client không cần biết từng service chạy port nào. Client chỉ gọi Gateway, Gateway route request tới service tương ứng.

Gateway cũng là nơi xử lý JWT security và public/private endpoint.
```

Ví dụ:

```text
/product/** -> Product Service
/order/** -> Order Service
/payment/** -> Payment Service
```

## 16. Eureka Có Vai Trò Gì?

Câu hỏi:

```text
Eureka dùng để làm gì?
```

Trả lời:

```text
Eureka là service registry.

Các service đăng ký với Eureka khi khởi động. Khi một service cần gọi service khác, nó có thể gọi bằng service name thay vì hard-code host/port.
```

Ví dụ:

```text
http://INVENTORY-SERVICE/internal/inventory/...
```

## 17. Những Điểm Mạnh Của Project

Có thể nói:

```text
Điểm mạnh của project là flow order-payment-inventory đã được tách khá rõ.

Inventory Service là source of truth cho stock.
Order Service không tự trừ stock.
Payment dùng Kafka event để notify Order và Inventory.
Search Service dùng Elasticsearch và update qua product event.
Gateway và Eureka giúp route/service discovery giống microservice thực tế.
```

Danh sách:

- Domain được chia theo service rõ.
- Có REST và Kafka.
- Có API Gateway.
- Có Eureka Discovery.
- Có Elasticsearch Search Service.
- Có Inventory reserve/confirm/release.
- Có xử lý race condition bằng pessimistic lock.
- Có idempotency cơ bản cho confirm/release.
- Có payment status transition rule.

## 18. Những Điểm Chưa Production

Nên trung thực khi phỏng vấn.

```text
Project này đang ở mức demo/pet project, chưa phải production-grade hoàn toàn.
```

Các điểm chưa production:

- Event DTO đang bị copy giữa service, chưa tách common-event module.
- Chưa có Outbox Pattern.
- Chưa có distributed tracing.
- Kafka retry/DLT mới có ở một số phần hoặc cần chuẩn hóa thêm.
- Chưa có migration bằng Flyway/Liquibase.
- Các port local chính đã được tách để chạy cùng lúc dễ hơn.
- Chưa có test coverage đầy đủ.

Nên nói tiếp:

```text
Nhưng em biết các điểm này và có roadmap cải thiện.
```

## 19. Nếu Được Cải Thiện Tiếp, Em Làm Gì?

Câu hỏi:

```text
Nếu có thêm thời gian, em sẽ cải thiện gì?
```

Trả lời theo ranking:

1. Tách common-event module để không copy event DTO.
2. Thêm Outbox Pattern cho payment/order/inventory event.
3. Chuẩn hóa retry + Dead Letter Topic cho Kafka consumer.
4. Thêm batch API ở Inventory để lấy stock nhiều product một lần.
5. Đổi `product.quantity` thành `cachedAvailableQuantity`.
6. Thêm Flyway/Liquibase migration.
7. Thêm distributed tracing bằng OpenTelemetry.
8. Thêm integration test cho flow order-payment-inventory.

## 20. Cách Demo Project Khi Phỏng Vấn

Thứ tự demo nên là:

```text
1. Mở README.md giải thích tổng quan.
2. Mở architecture.md chỉ flow order-payment-inventory.
3. Chạy Eureka/Gateway/service nếu cần.
4. Tạo product.
5. Tạo inventory.
6. Gọi product detail để thấy quantity lấy từ inventory.
7. Tạo order.
8. Kiểm tra order status PENDING_PAYMENT.
9. Mark payment success.
10. Kiểm tra order CONFIRMED.
11. Kiểm tra inventory soldQuantity tăng.
12. Kiểm tra product/search sync stock.
```

Nếu không chạy demo live được, vẫn có thể trình bày bằng flow trong tài liệu.

## 21. Câu Trả Lời Mẫu 1 Phút

Nếu chỉ có 1 phút để giới thiệu:

```text
Project của em là e-commerce backend theo microservices bằng Spring Boot.

Em tách các domain chính thành service riêng như Product, Inventory, Order, Payment, Search, Identity, Gateway và Discovery.

Flow chính là user tạo order, Order Service lấy thông tin sản phẩm từ Product, sau đó gọi Inventory để reserve hàng. Nếu reserve thành công thì order chuyển sang PENDING_PAYMENT. Khi payment success, Payment Service publish Kafka event, Order Service consume để confirm order, Inventory Service consume để confirm stock và chuyển reservedQuantity sang soldQuantity.

Em thiết kế Inventory Service là source of truth cho tồn kho. Product Service chỉ giữ quantity denormalized để list nhanh, còn product detail hoặc checkout sẽ lấy stock chính xác từ Inventory.

Project dùng REST cho bước cần phản hồi ngay và Kafka cho event bất đồng bộ.
```

## 22. Câu Trả Lời Mẫu 3 Phút

Nếu có 3 phút:

```text
Đây là project e-commerce microservices em xây bằng Spring Boot.

Các service chính gồm API Gateway, Eureka Discovery, Identity, Profile, Product, Search, Inventory, Order, Payment và Notification.

Em dùng API Gateway làm entry point, Eureka để service discovery. Các service gọi nhau bằng REST khi cần kết quả ngay, ví dụ Order gọi Inventory để reserve hàng. Kafka được dùng cho các event bất đồng bộ như product-created, payment-success, inventory-updated.

Flow quan trọng nhất là order-payment-inventory. Khi user tạo order, Order Service lấy product detail từ Product Service để biết tên, giá, status. Sau đó Order lưu trạng thái PENDING và gọi Inventory Service reserve. Nếu đủ hàng, Inventory giảm availableQuantity, tăng reservedQuantity và Order chuyển sang PENDING_PAYMENT. Nếu thiếu hàng, Order chuyển sang INVENTORY_FAILED.

Khi user thanh toán thành công, Payment Service chuyển payment sang SUCCESS và publish payment-success. Order Service nghe event này để chuyển order sang CONFIRMED. Inventory Service cũng nghe event này để confirm reservation, giảm reservedQuantity và tăng soldQuantity. Sau đó Inventory publish inventory-updated để Product Service sync quantity, rồi Product publish product-updated cho Search Service cập nhật inStock.

Điểm em chú ý là Inventory Service là source of truth cho stock. Product Service vẫn có quantity nhưng đó là bản sao denormalized để list nhanh, không dùng để quyết định còn hàng khi checkout.

Project hiện là pet project nên vẫn có các điểm cần cải thiện như tách common-event module, thêm Outbox Pattern, retry/DLT đầy đủ và tracing.
```

## 23. Checklist Trước Khi Đi Phỏng Vấn

Trước khi demo hoặc nộp project, nên kiểm tra:

- README đã mô tả rõ project.
- `architecture.md` đã có flow chính.
- Product Service không còn trừ stock theo `order-created`.
- Inventory Service publish `inventory-updated`.
- Product Service consume `inventory-updated`.
- Payment status không chuyển sai.
- Order reserve fail không bị rollback mất status `INVENTORY_FAILED`.
- Các service chính compile được.
- Biết giải thích REST vs Kafka.
- Biết nói rõ điểm chưa production.

## 24. Một Số Câu Hỏi Nhanh Và Trả Lời Nhanh

### Stock thật nằm ở đâu?

```text
Inventory Service.
```

### Product.quantity là gì?

```text
Bản sao denormalized từ Inventory, dùng để list/filter nhanh.
```

### Khi checkout dùng quantity nào?

```text
Inventory.availableQuantity.
```

### Vì sao dùng Kafka?

```text
Để publish event bất đồng bộ và giảm coupling giữa service.
```

### Vì sao dùng REST?

```text
Cho các thao tác cần kết quả ngay, như reserve inventory.
```

### Payment success ảnh hưởng service nào?

```text
Order Service confirm order, Inventory Service confirm reservation.
```

### Payment failed thì sao?

```text
Inventory Service release hàng đã reserve.
```

### Search có luôn đúng tức thì không?

```text
Không tuyệt đối. Search là read model eventual consistency, được cập nhật qua event.
```

### Project đã production-ready chưa?

```text
Chưa hoàn toàn. Đây là pet project/demo, nhưng có hướng nâng cấp như Outbox, DLT, tracing, migration và test.
```
