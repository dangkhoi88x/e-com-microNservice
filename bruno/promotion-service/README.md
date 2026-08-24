# NovaShop Promotion Service – Bruno

Chọn environment `local`, sau đó chạy request theo thứ tự:

1. `01-auth/Login` và sửa `email/password` nếu cần. Request sẽ lưu `accessToken` vào biến runtime.
2. `02-campaign/Create campaign`, sau đó `Activate campaign`. Campaign mẫu là `NOVA20`, giảm 20%, đơn tối thiểu 500.000đ.
3. `03-internal/Validate`, `Reserve`, `Confirm` để kiểm tra trực tiếp promotion-service. `Release` dùng với một reservation chưa confirm.
4. Để test checkout, cart phải có sản phẩm đang `selected=true`. Chạy `04-order-integration/Checkout with promotion` để kiểm tra Order Service gọi validate/reserve. Request tự lưu `orderId`.
5. Dùng `04-order-integration/Confirm promotion for order` để mô phỏng payment SUCCESS. Dùng `Release promotion for order` cho luồng FAILED/CANCELLED.

Yêu cầu service đang chạy: Eureka `8761`, API Gateway `9191`, promotion-service `8095`, order-service và cart-service. CRUD đi qua Gateway; internal promotion endpoint gọi trực tiếp port `8095` và chỉ dùng cho service-to-service test.

Nếu login của bạn dùng tài khoản khác, sửa biến `email`, `password` trong environment hoặc ngay trong request Login.
