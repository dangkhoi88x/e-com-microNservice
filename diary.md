# NovaShop Development Diary

Nhật ký này lưu lại quá trình phát triển, các quyết định kỹ thuật/nghiệp vụ và những vấn đề cần tiếp tục của project NovaShop. Git vẫn là nguồn chính xác cho từng dòng code; `diary.md` cung cấp bối cảnh để người hoặc agent tiếp theo hiểu **vì sao** project được xây theo hướng hiện tại.

Tài liệu liên quan:

- `start_here.md`: hướng dẫn đọc và chạy toàn bộ project.
- `agent_context.md`: ràng buộc kiến trúc và working contract cho AI agent.
- `architecture.md`: mô tả kiến trúc và luồng giao tiếp.

---

## 2026-07-22 — Ổn định Promotion và bổ sung tài liệu dự án

### Đã làm

- Kiểm tra lại repository sau khi merge từ remote.
- Xác nhận commit ngày 21/07 vẫn là tổ tiên của `main`, không mất code nghiệp vụ sau merge.
- Kiểm tra IntelliJ shelf trước update; shelf chỉ chứa `.idea/workspace.xml`, không chứa Java/React source.
- Chẩn đoán lỗi trang `/promotions` tải campaign lúc được lúc không.
- Xác nhận Promotion Service trực tiếp vẫn healthy và Gateway route tồn tại.
- Phát hiện Eureka có hai instance Promotion cùng lúc:
  - Một service chạy local/IntelliJ ở port 8095.
  - Một container Docker publish port 8094 vào container port 8095.
- Kết luận Gateway load-balance qua hai instance làm hành vi chập chờn, đặc biệt khi một instance hoặc advertised address không ổn định.
- Thống nhất cách chạy local: giữ PostgreSQL Promotion trong Docker, chạy application Promotion bằng IntelliJ; không chạy hai application instance cùng lúc.
- Xoá khối `Recent activity`, `Product catalog sync` và `Order status pipeline` khỏi sidebar admin.
- Tạo `start_here.md` làm tài liệu onboarding toàn project.
- Tạo `agent_context.md` làm context cho các AI coding agent.
- Khởi tạo nền tảng `shipping-service` đến Service interface:
  - PostgreSQL riêng ở port 5438 và service port 8096.
  - `AbstractEntity`, `Shipment`, `ShipmentHistory`, `ShipmentStatus`.
  - Request/response DTO, MapStruct mapper và repository.
  - Exception contract và `ShipmentService` interface.
  - Optimistic locking, unique order/tracking constraints và timeline relation.
- Thêm Shipping vào root Maven modules; build Shipping thành công với Java 25.

### Quyết định

- Docker ưu tiên chạy infrastructure/database.
- Spring Boot services ưu tiên chạy bằng IntelliJ/Maven trong môi trường local hiện tại.
- Dừng `promotion-service` container không làm mất database vì `promotion-postgres` và named volume là thành phần riêng.
- Không dùng frontend retry để che một lỗi Eureka/instance kéo dài; cần xử lý registration/runtime trước.

### Kiểm chứng

- Promotion direct health từng trả HTTP 200.
- Gateway nhận route Promotion; request không token trả 401 đúng kỳ vọng.
- Frontend build thành công sau khi cập nhật sidebar.
- `start_here.md` và `agent_context.md` được kiểm tra UTF-8 và không chứa credential thật.

### Còn mở

- Chọn một nguồn duy nhất cho Gateway routes: Java hoặc YAML.
- Thêm `promotion-service` vào root Maven modules.
- Chuẩn hoá advertised hostname/port nếu muốn chạy application services bằng Docker.
- Di chuyển secret còn hardcode trong cấu hình sang environment variables.

---

## 2026-07-21 — Promotion, Flash Sale và storefront deal

Commit tham chiếu: `3f5f4e0`.

### Đã làm

- Mở rộng Promotion Service cho flash deal và long-term sale.
- Thêm các entity/flow liên quan:
  - `FlashDeal`.
  - `FlashDealItem`.
  - `FlashDealReservation`.
  - `FlashDealNotificationSubscription`.
  - `PromotionClaim`.
- Thêm API quản trị flash deal và API nội bộ reserve/confirm/release.
- Thêm số liệu/detail response cho màn hình Flash Deal admin.
- Tích hợp flash-deal price/reservation vào Order Service.
- Thêm event/consumer notification cho flash sale.
- Cập nhật storefront:
  - Flash sale carousel.
  - Best Deals.
  - Hot Deals.
  - Checkout hiển thị deal/promotion.
  - Header, mini cart và notification UI.
- Cập nhật admin Promotions và Flash Deals.

### Quyết định

- Giá flash deal phải được backend Promotion xác thực và reserve; frontend chỉ hiển thị.
- Deal reservation phải được confirm khi payment thành công và release khi payment thất bại/huỷ.
- Flash deal có lifecycle riêng: `DRAFT -> SCHEDULED -> LIVE -> ENDED/SOLD_OUT`.

---

## 2026-07-20 — Promotion Service và tích hợp Order/Payment

Commit tham chiếu: `ce4056b`, sau đó được merge cùng các thay đổi frontend/cart/wishlist.

### Promotion Service

- Xây dựng `PromotionCampaign` CRUD cho admin.
- Dùng MapStruct và tách mapper để service implementation gọn hơn.
- Cho entity kế thừa `AbstractEntity` để dùng `id`, `createdAt`, `updatedAt` thống nhất.
- Bổ sung error handling chuẩn:
  - `ErrorCode`.
  - Service exception.
  - `ErrorResponse`.
  - `GlobalExceptionHandler`.
- Bổ sung các lỗi nghiệp vụ như code trùng, không active, hết hạn, sai thời gian và hết usage limit.
- Thêm internal API:

```text
POST /internal/promotions/validate
POST /internal/promotions/reserve
POST /internal/promotions/confirm
POST /internal/promotions/release
```

- Thiết kế `PromotionUsage` theo trạng thái `RESERVED`, `USED`, `RELEASED`.
- Thêm Dockerfile, PostgreSQL Promotion và service configuration trong Compose.
- Tạo Bruno collection để test campaign, validate, reserve, confirm, release và checkout integration.

### Order và Promotion

- Checkout nhận `campaignCode`.
- Order lưu:
  - `subtotalAmount`.
  - `discountAmount`.
  - `promotionCode`.
  - `totalAmount` sau giảm giá.
- Order gọi Promotion validate/reserve trong checkout.
- Payment success gọi Promotion confirm.
- Payment failed/cancelled gọi Promotion release.
- Các thao tác được thiết kế idempotent theo `orderId`.

### Admin Promotions

- Tạo route `/promotions`.
- Thêm menu Promotions trong Store Service.
- Thêm trang danh sách, metrics, filter, create/edit, activate/deactivate và delete campaign.
- Chưa ép role ADMIN ở frontend; hiện chỉ yêu cầu đăng nhập theo quyết định tạm thời.

### Sự cố đã gặp

- Gateway trả 404 cho Promotion vì route runtime nằm trong `GatewayConfiguration.java`, không chỉ YAML.
- Sau khi thêm route Java, API cần restart Gateway.
- Promotion database chưa kết nối vì container/app port và datasource chưa thống nhất.
- Đã tách host port Docker `8094` khỏi application port `8095`.
- Trang Promotions đôi lúc báo sai “service not running”; frontend được bổ sung phân loại lỗi auth và retry ngắn cho lỗi Gateway tạm thời.

---

## 2026-07-19 — Storefront, Wishlist và trải nghiệm mua hàng

Commit tham chiếu: `77692f8`, `f25fb81`, `def23a3` và các commit frontend liên quan.

### Storefront

- Xây dựng landing page NovaShop theo palette White × Baby Blue × Deep Blue.
- Chuyển navigation storefront từ sidebar sang header ngang.
- Header hỗ trợ:
  - Search/autocomplete.
  - Wishlist.
  - Notification.
  - Cart preview.
  - Login/register hoặc logout tuỳ trạng thái phiên.
- Tạo product detail route theo slug.
- Thêm breadcrumb, product specifications và product description.
- Thêm related products.
- Tạo category page với filter/search.
- Tạo Hot Deal/Best Deal/Flash Sale sections.
- Thêm cart page, checkout page, account page và customer order history.

### Wishlist

- Tách `wishlist-service` riêng.
- Thêm entity/repository/service/controller và security.
- Bổ sung validate product/variant qua Product Service.
- Backend tự lấy snapshot tên, giá và ảnh thay vì tin payload frontend.
- Thêm Dockerfile và Compose configuration.
- Kết nối trái tim storefront/product detail với backend.
- Thêm trang `/shop/wishlist`.
- “Thêm vào giỏ” từ wishlist chỉ mutate cart, không tự chuyển trang cart.
- Cân bằng chiều cao card và vị trí action.

### Auth khách hàng

- Tách giao diện customer login/register khỏi admin.
- Thêm redirect trở lại checkout sau đăng nhập.
- Xây dựng account/profile riêng cho storefront, không điều hướng sang admin `/profile`.

---

## 2026-07-18 — Product Variant, options động và checkout UI

### Product model

- Mở rộng Product theo mô hình SKU/variant.
- Thêm:
  - `ProductVariant`.
  - `ProductOption`.
  - `ProductOptionValue`.
- Variant dùng `attributes` để lưu tổ hợp như Color, Size, Storage.
- Create/Update request và Product detail response được nối với options/variants.
- Thêm validate để variant chỉ dùng option value hợp lệ của product.
- Cập nhật form Create/Edit Product để admin nhập option động thay vì hardcode color/size/storage.

### Product detail và checkout

- Product detail hỗ trợ chọn biến thể trước khi mua/thêm cart.
- Route detail chuyển từ UUID trực tiếp sang slug.
- Checkout được thiết kế lại theo phong cách marketplace hiện đại.
- Cho phép sửa quantity ở phần “Đơn hàng của bạn”.
- Loại bỏ link “Chỉnh sửa” không cần thiết khỏi checkout summary.

### Quyết định

- `Product` là catalog-level aggregate.
- `ProductVariant` là SKU thực tế để bán.
- Các service Cart/Order/Wishlist/Inventory cần giữ `variantId` khi sản phẩm có biến thể.

---

## 2026-07-17 — Cart Service và checkout locking

Commit tham chiếu: `3d9f091`.

### Cart Service

- Tạo Cart/CartItem theo cấu trúc service hiện có.
- Entity sử dụng abstract/base entity.
- Hoàn thiện request, response, mapper, repository, service và controller.
- Thêm route Cart qua API Gateway.
- Kết nối frontend cart.

### Checkout locking

- Thêm `checkoutOrderId` vào CartItem.
- Checkout lấy selected items rồi đánh dấu đúng item theo order.
- Thêm internal endpoints:

```text
POST /internal/cart/{orderId}/finalize
POST /internal/cart/{orderId}/release
```

- Payment success chỉ xoá item có `checkoutOrderId` tương ứng.
- Payment failed/cancelled chỉ bỏ lock của order đó.
- Không xoá toàn bộ item `selected=true` vì user có thể thêm/chọn item mới trong lúc thanh toán.

### Quyết định

- Internal correlation tiếp tục dùng `orderId` UUID.
- Có thảo luận chuyển toàn bộ sang `orderCode`, nhưng quyết định cuối cùng là **không đổi**.

---

## 2026-07-16 — Hoàn thiện admin UI và sửa integration service

Commit tham chiếu: `0ca7643`, `47f9772`, `8b63288`.

### Backend/integration

- Sửa nhiều lỗi khởi động Inventory, Search, Notification và Authentication.
- Sửa gRPC token introspection giữa API Gateway và Identity.
- Xử lý dependency/generated package cho `IntrospectServiceGrpc`.
- Sửa Inventory unauthorized và reserve flow.
- Chẩn đoán Order `INVENTORY_FAILED` khi không reserve được stock.
- Hoàn thiện luồng complete order và đối chiếu `reservedQuantity`/`soldQuantity`.

### Admin frontend

- Thiết kế lại table Product theo phong cách admin hiện đại.
- Áp dụng UI chung cho Products, Categories, Search và Orders.
- Thêm action Complete order.
- Tạo Product create/edit form dọc, responsive và dễ đọc.
- Sửa các ô filter bị co nhỏ/ẩn text.
- Làm search panel ngang.
- Bổ sung notification admin khi có order mới/thành công.
- Tích hợp phong cách shadcn/MUI, tăng font table và cải thiện page header.
- Hoàn thiện trang admin Profile.

---

## 2026-07-09 — Order, Inventory và Payment consistency

Commit tham chiếu: `44968f4`, `37b3e66`.

### Đã làm

- Sửa Inventory client/RestClient bean integration.
- Sửa order status constraint.
- Sửa event consumers và Kafka retry/repository structure.
- Khi order bị huỷ, inventory reservation được release.
- Khi payment thành công, inventory reservation được confirm.
- Bắt đầu chuẩn hoá trạng thái và compensation giữa Order, Payment và Inventory.

---

## 2026-07-05 đến 2026-07-08 — Search và admin foundation

### Đã làm

- Search Service consume product events và index Elasticsearch.
- Thêm pagination/filter/specification cho product/search.
- Xây dựng admin frontend ban đầu.
- Kết nối Order Service với frontend admin.
- Bổ sung Kafka retry và refactor repository.

---

## 2026-06-18 đến 2026-06-29 — Nền tảng project

### Đã làm

- Tạo Notification Service.
- Tạo JWT filter và authentication flow.
- Product/Category CRUD và decode token.
- Product role/security bước đầu.
- Xây các page/filter/specification ban đầu.
- Tạo frontend foundation.

---

## Các quyết định kiến trúc đang có hiệu lực

1. Inventory Service là source of truth của stock.
2. Search là read model; không dùng Elasticsearch để xác nhận stock khi checkout.
3. Product Variant là SKU thực tế.
4. Order lưu snapshot sản phẩm/giá tại thời điểm mua.
5. Cross-service correlation dùng `orderId`, không dùng `orderCode` thay thế toàn bộ.
6. Cart finalize/release dựa trên `checkoutOrderId` cụ thể.
7. Payment event có thể bị gửi lặp; consumer phải idempotent.
8. Promotion/Flash Deal phải reserve trước payment và confirm/release theo kết quả payment.
9. Frontend không được là nguồn authoritative cho giá, discount, stock hay product snapshot.
10. Admin hiện mới kiểm tra authentication; role enforcement sẽ làm sau.

## Backlog ưu tiên

### Kiến trúc và vận hành

- [ ] Thêm `promotion-service` vào root Maven modules.
- [ ] Chọn Java hoặc YAML làm nguồn Gateway routes duy nhất.
- [ ] Chuẩn hoá Docker networking và Eureka advertised address.
- [ ] Tách database/schema cho Cart, Order, Payment và Wishlist.
- [ ] Thêm Flyway/Liquibase.
- [ ] Di chuyển toàn bộ secret sang environment/secret store.
- [ ] Thêm centralized logging, tracing và correlation ID.

### Nghiệp vụ

- [ ] Product Review/Rating Service.
- [ ] Payment gateway/webhook thực tế thay cho endpoint mô phỏng.
- [ ] Shipping/fulfillment flow hoàn chỉnh.
- [ ] Refund/return flow.
- [ ] Promotion stacking rule và usage limit theo user rõ ràng hơn.
- [ ] Expire checkout reservation theo timeout.

### Chất lượng

- [ ] Integration test checkout success/failure compensation.
- [ ] Test duplicate Kafka delivery/idempotency.
- [ ] Contract test giữa Order và Cart/Inventory/Promotion.
- [ ] Route-level lazy loading cho frontend.
- [ ] Chuẩn hoá Spring Boot version giữa các module.

## Mẫu ghi nhật ký cho lần tiếp theo

Sao chép block sau và đặt entry mới nhất lên trên:

```markdown
## YYYY-MM-DD — Tên thay đổi

### Mục tiêu

- ...

### Đã làm

- ...

### Quyết định

- ...

### Kiểm chứng

- Command/test: kết quả.

### Còn mở

- [ ] ...
```

Khi cập nhật nhật ký:

- Ghi lý do và quyết định, không chép nguyên diff.
- Chỉ đánh dấu “đã hoàn thành” khi build/test hoặc kiểm chứng hành vi đã thành công.
- Không ghi access token, refresh token, password, API key hoặc dữ liệu cá nhân.
- Nếu thông tin khác Git, ưu tiên Git/source code và sửa lại nhật ký.
