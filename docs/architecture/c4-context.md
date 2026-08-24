# C4 Context — NovaShop

## Mục tiêu

Context diagram mô tả ranh giới hệ thống NovaShop và các actor/hệ thống ngoài có bằng chứng trong source. Chi tiết triển khai nằm ở [C4 Containers](c4-containers.md).

![C4 Context NovaShop](diagrams/c4-context.png)

```mermaid
C4Context
  title Context hệ thống NovaShop

  Person(customer, "Khách hàng", "Duyệt catalog, giỏ hàng, checkout và đơn hàng")
  Person(seller, "Người bán", "Quản lý shop, product và variant")
  Person(admin, "Quản trị viên", "Quản lý catalog, order, payment và promotion")

  System(novashop, "NovaShop", "Nền tảng e-commerce gồm frontend và Spring Boot microservices")
  System_Ext(stripe, "Stripe", "Checkout session và webhook payment")
  System_Ext(s3, "S3-compatible object storage", "Lưu media asset")
  System_Ext(smtp, "SMTP provider", "Gửi email notification")

  Rel(customer, novashop, "Mua hàng qua", "HTTPS")
  Rel(seller, novashop, "Quản lý hàng hoá qua", "HTTPS")
  Rel(admin, novashop, "Vận hành qua", "HTTPS")
  Rel(novashop, stripe, "Tạo checkout session và nhận webhook", "HTTPS")
  Rel(novashop, s3, "Lưu/lấy media asset", "S3 API")
  Rel(novashop, smtp, "Gửi email notification", "SMTP")
```

## Evidence

- Actor/UI: `web-app/src/routes/AppRoutes.jsx` và các `web-app/src/pages/*`.
- Stripe: `payment-service/.../PaymentController.java` và `PaymentServiceImpl.java` có Stripe checkout, reconcile và webhook.
- Object storage: `media-service/.../configuration/S3Configuration.java` và `S3Properties.java`.
- Email: `notification-service/src/main/resources/templates/` và `notification-service/pom.xml` có mail/Thymeleaf dependencies.

## Chưa xác minh

Provider cụ thể của S3-compatible storage và SMTP được lấy từ environment/config runtime; không có giá trị credential trong tài liệu này.
