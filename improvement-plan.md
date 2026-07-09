# Improvement Plan - Khoi Microservice E-commerce

Tài liệu này là roadmap cải thiện project `Khoi-Micro`.

Mục tiêu:

- Biết service/flow nào cần cải thiện trước.
- Biết nên mở rộng service nào tiếp theo.
- Giữ project ở mức hợp lý cho pet project/phỏng vấn.
- Không làm quá sâu kiểu production khi chưa cần.

## 1. Nguyên Tắc Ưu Tiên

Khi cải thiện project, nên ưu tiên theo thứ tự:

```text
1. Fix lỗi nghiệp vụ dễ bị hỏi.
2. Làm flow chính chạy mượt end-to-end.
3. Làm documentation và demo flow rõ ràng.
4. Thêm test cho flow quan trọng.
5. Sau đó mới mở rộng service mới.
```

Với project này, flow quan trọng nhất là:

```text
Product -> Inventory -> Order -> Payment -> Inventory/Product/Search
```

Nếu flow này chắc, project đã đủ mạnh để đem đi phỏng vấn.

## 2. Tình Trạng Hiện Tại

Các phần đã có:

- API Gateway.
- Eureka Discovery.
- Identity/Auth.
- Profile.
- Product/category.
- Search với Elasticsearch.
- Inventory reserve/confirm/release.
- Order tạo đơn và reserve inventory.
- Payment success/failed/cancelled.
- Kafka event cho product/payment/inventory/order.
- Product/Search sync stock từ Inventory event.
- README, architecture notes, interview notes.

Các quyết định kiến trúc hiện tại:

```text
Inventory Service = source of truth cho stock.
Product Service = catalog + quantity denormalized.
Search Service = read model cho search/filter.
Order Service = tạo order và gọi inventory reserve.
Payment Service = publish payment event.
```

## 3. Priority 1 - Cần Cải Thiện Ngay

Đây là nhóm nên làm trước vì liên quan trực tiếp đến flow chính và dễ bị hỏi khi phỏng vấn.

### 3.1. Chuẩn Hóa Response Khi Order Inventory Failed

Hiện order reserve fail sẽ trả order với status:

```text
INVENTORY_FAILED
```

Cần kiểm tra controller message để client không hiểu nhầm là order thành công hoàn toàn.

Nên làm:

```text
Nếu data.status = INVENTORY_FAILED
-> message = "Order created but inventory reservation failed"

Nếu data.status = PENDING_PAYMENT
-> message = "Order created successfully"
```

Lý do:

- Client dễ hiểu.
- Demo rõ hơn.
- Không bị hỏi vì sao API trả success nhưng status failed.

Mức độ: Rất nên làm.

### 3.2. Giới Hạn API Update Order Status

Hiện API update status có thể cho đổi trạng thái order trực tiếp.

Vấn đề:

```text
Có thể bypass flow payment/inventory.
```

Ví dụ không nên cho:

```text
PENDING -> CONFIRMED
INVENTORY_FAILED -> CONFIRMED
CANCELLED -> PENDING_PAYMENT
```

Nên làm:

- Chỉ admin được dùng API này.
- Hoặc tạm bỏ khỏi public flow demo.
- Hoặc validate transition rõ ràng.

Mức độ: Rất nên làm.

### 3.3. Chuẩn Hóa Payment Flow Sau Khi Payment Failed

Hiện Payment failed/cancelled đã publish event để Inventory release.

Cần kiểm tra thêm:

```text
Order có nên đổi sang CANCELLED/PAYMENT_FAILED không?
```

Hiện flow chính mới confirm order khi payment success. Với payment failed, inventory release là đúng, nhưng order status nếu vẫn `PENDING_PAYMENT` thì hơi thiếu rõ.

Nên thêm:

```text
payment-failed -> Order Service consume -> order status PAYMENT_FAILED hoặc CANCELLED
payment-cancelled -> Order Service consume -> order status CANCELLED
```

Nếu chưa muốn thêm status mới:

```text
payment-failed -> CANCELLED
payment-cancelled -> CANCELLED
```

Mức độ: Rất nên làm.

### 3.4. Thêm Test Hoặc Postman Flow Cho Order-Payment-Inventory

Nếu chưa viết test tự động, ít nhất nên có file hướng dẫn demo bằng Postman.

Nên tạo:

```text
api-docs.md
```

Nội dung:

- Login lấy token.
- Tạo category.
- Tạo product.
- Tạo inventory.
- Tạo order.
- Tạo payment.
- Mark success.
- Check inventory.

Mức độ: Rất nên làm.

## 4. Priority 2 - Nên Cải Thiện Sau Khi Flow Chính Ổn

Nhóm này giúp project sạch hơn và gần thực tế hơn, nhưng chưa bắt buộc ngay.

### 4.1. Tách Common Event Module

Hiện event DTO đang bị copy giữa nhiều service.

Ví dụ:

```text
PaymentSuccessEvent
PaymentFailedEvent
InventoryUpdatedEvent
ProductUpdatedEvent
```

Vấn đề:

- Dễ lệch field giữa producer và consumer.
- Sửa event phải sửa nhiều service.
- Khó maintain.

Nên tạo module:

```text
common-event
```

Chứa:

- Product events.
- Payment events.
- Inventory events.
- Order events.
- User/profile events.

Mức độ: Nên làm.

### 4.2. Chuẩn Hóa Retry Và Dead Letter Topic Cho Kafka

Hiện một số Kafka consumer đã có hướng retry/DLT nhưng chưa đồng bộ toàn hệ thống.

Nên chuẩn hóa:

```text
payment-success.DLT
payment-failed.DLT
payment-cancelled.DLT
inventory-updated.DLT
product-updated.DLT
```

Lý do:

- Nếu consumer lỗi, event không bị mất.
- Dễ debug.
- Phỏng vấn rất dễ được điểm.

Mức độ: Nên làm.

### 4.3. Batch API Cho Inventory - Đã Làm

Inventory Service đã có batch API để Product Service lấy stock của nhiều product trong một request.

API:

```text
POST /api/v1/inventory/products/batch
```

Request:

```json
{
  "productIds": ["p1", "p2", "p3"]
}
```

Response:

```json
[
  {
    "productId": "p1",
    "availableQuantity": 10
  }
]
```

Product Service có thể dùng API này cho product list để tránh gọi Inventory Service từng product một.

### 4.4. Đổi Tên Product Quantity

Hiện `product.quantity` là bản sao từ inventory.

Tên tốt hơn:

```text
cachedAvailableQuantity
```

Hoặc:

```text
availableQuantity
```

Lý do:

- Đọc code dễ hiểu hơn.
- Không bị nhầm Product là source of truth.

Mức độ: Nên làm, nhưng sửa nhiều chỗ nên làm sau.

### 4.5. Thêm Flyway Hoặc Liquibase

Hiện các service dùng:

```text
ddl-auto: update
```

Điều này tiện cho demo nhưng chưa tốt cho production.

Nên thêm migration cho:

- Product DB.
- Inventory DB.
- Order DB.
- Payment DB.

Mức độ: Nên làm sau.

## 5. Priority 3 - Mở Rộng Service Tiếp Theo

Sau khi flow e-commerce chính ổn, có thể mở rộng thêm service.

### 5.1. Cart Service

Đây là service nên làm tiếp theo nhất.

Lý do:

- E-commerce thường có cart trước order.
- Dễ demo.
- Không làm flow hiện tại quá phức tạp.

Cart Service quản lý:

- Thêm sản phẩm vào giỏ.
- Xóa sản phẩm khỏi giỏ.
- Cập nhật quantity trong giỏ.
- Xem giỏ hàng.
- Checkout cart thành order.

Flow:

```text
User add product to cart
-> Cart Service lưu cart item
-> User checkout
-> Cart Service gọi Order Service create order
```

Lưu ý:

- Cart chưa cần reserve inventory.
- Chỉ khi checkout tạo order mới reserve inventory.

Mức độ: Rất nên làm nếu muốn mở rộng.

### 5.2. Shipping Service

Shipping Service phù hợp sau Payment/Order.

Quản lý:

- Tạo shipment khi order confirmed.
- Trạng thái giao hàng.
- Tracking number.
- Delivered/failed delivery.

Flow:

```text
payment-success
-> order CONFIRMED
-> Shipping Service tạo shipment
```

Trạng thái gợi ý:

```text
PENDING
PACKING
SHIPPING
DELIVERED
FAILED
RETURNED
```

Mức độ: Nên làm sau Cart.

### 5.3. Review/Rating Service

Review Service giúp project có thêm nghiệp vụ user-facing.

Quản lý:

- User đánh giá sản phẩm.
- Rating 1-5 sao.
- Comment.
- Chỉ cho review nếu order đã confirmed/delivered.

Flow:

```text
User mua hàng thành công
-> Sau khi delivered
-> User được review product
```

Mức độ: Nên làm sau Shipping.

### 5.4. Promotion/Coupon Service

Service này giúp project giống e-commerce thật hơn.

Quản lý:

- Coupon code.
- Discount theo phần trăm.
- Discount theo số tiền.
- Điều kiện min order amount.
- Ngày hết hạn.

Flow:

```text
User tạo order
-> Order Service gọi Promotion Service validate coupon
-> Tính discount
-> Lưu totalAmount sau giảm giá
```

Mức độ: Làm sau khi Order/Payment ổn.

### 5.5. Admin/Reporting Service

Service này phục vụ dashboard.

Quản lý:

- Tổng doanh thu.
- Tổng order.
- Sản phẩm bán chạy.
- Tồn kho thấp.
- Payment success/fail rate.

Có thể consume event:

```text
order-created
payment-success
inventory-updated
```

Mức độ: Nên làm nếu muốn show dashboard/reporting.

## 6. Roadmap Gợi Ý

### Phase 1 - Dọn Flow Hiện Tại

Mục tiêu:

```text
Order - Payment - Inventory chạy rõ ràng, dễ demo.
```

Việc cần làm:

1. Fix response message khi order `INVENTORY_FAILED`.
2. Giới hạn API update order status.
3. Cho Order Service xử lý `payment-failed/payment-cancelled`.
4. Viết `api-docs.md` để demo Postman.

Kết quả mong muốn:

```text
Flow mua hàng thành công/thất bại đều rõ.
Không có status treo khó hiểu.
```

### Phase 2 - Làm Sạch Kiến Trúc

Mục tiêu:

```text
Code dễ maintain hơn.
```

Việc cần làm:

1. Tách `common-event`.
2. Chuẩn hóa Kafka retry/DLT.
3. Đổi tên `product.quantity` thành `cachedAvailableQuantity`.
4. Đổi tên `product.quantity` thành `cachedAvailableQuantity`.

Kết quả mong muốn:

```text
Ít duplicate code.
Event schema rõ.
Stock read model dễ hiểu hơn.
```

### Phase 3 - Mở Rộng Service

Mục tiêu:

```text
Project giống e-commerce thật hơn.
```

Thứ tự nên làm:

1. Cart Service.
2. Shipping Service.
3. Review/Rating Service.
4. Promotion/Coupon Service.
5. Reporting Service.

## 7. Ranking Việc Nên Làm Ngay

Nếu chỉ chọn 7 việc gần nhất, nên làm:

1. Fix response message khi order `INVENTORY_FAILED`.
2. Cho Order Service consume `payment-failed/payment-cancelled`.
3. Giới hạn hoặc validate `updateOrderStatus`.
4. Viết `api-docs.md`.
5. Tách `common-event` module.
6. Chuẩn hóa Kafka retry/DLT.
7. Bắt đầu Cart Service.

## 8. Cách Trả Lời Khi Bị Hỏi "Project Này Nên Làm Gì Tiếp?"

Câu trả lời mẫu:

```text
Nếu cải thiện tiếp, em sẽ ưu tiên làm chắc flow order-payment-inventory trước.

Cụ thể là xử lý rõ payment failed/cancelled để order không bị treo ở PENDING_PAYMENT, giới hạn API update order status để không bypass nghiệp vụ, và viết api-docs để demo end-to-end.

Sau đó em sẽ tách common-event module, chuẩn hóa retry/DLT cho Kafka, rồi mới mở rộng Cart Service vì đây là service tự nhiên tiếp theo trong e-commerce.
```
