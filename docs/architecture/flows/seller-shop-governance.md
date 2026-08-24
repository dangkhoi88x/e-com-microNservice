# Flow — Duyệt và suspend seller shop

## Scope

Seller tạo shop ở trạng thái `PENDING`. Admin duyệt, từ chối hoặc suspend qua Seller Service. Nhánh APPROVE gọi Identity Service để cấp role seller; mọi thay đổi trạng thái đều phát `seller-shop-status-changed`. Product Service chỉ phản ứng với trạng thái `SUSPENDED`: inactivate product/variant đang ACTIVE và phát `product-updated` cho từng product đã đổi.

![Sequence quản trị seller shop](../diagrams/seller-shop-governance-sequence.png)

```mermaid
sequenceDiagram
    actor Seller as Người bán
    actor Admin as Quản trị viên
    participant GW as API Gateway
    participant SellerSvc as Seller Service
    participant SellerDB as Seller database
    participant Identity as Identity Service
    participant Kafka as Kafka
    participant Product as Product Service
    participant ProductDB as Product database

    Seller->>GW: POST /seller/api/v1/sellers/me/shop
    GW->>SellerSvc: Forward REST/HTTPS
    SellerSvc->>SellerDB: Lưu shop PENDING
    Admin->>GW: PUT /seller/api/v1/sellers/admin/{shopId}/review
    GW->>SellerSvc: Forward review action
    alt APPROVE
        SellerSvc->>Identity: Grant seller role (REST)
        Identity-->>SellerSvc: Role granted
    end
    SellerSvc->>SellerDB: Lưu trạng thái review
    SellerSvc->>Kafka: Publish seller-shop-status-changed
    opt Status SUSPENDED
        Kafka-->>Product: Deliver status event
        Product->>ProductDB: Inactivate active products/variants
        Product->>Kafka: Publish product-updated cho từng product
    end
```

## Activity: transition seller shop

![Activity transition seller shop](../diagrams/seller-shop-governance-activity.png)

```mermaid
flowchart TD
    start([Admin review shop]) --> current{"Trạng thái hiện tại hợp lệ?"}
    current -->|Không| invalid["Trả INVALID_SELLER_TRANSITION"]
    current -->|Có| action{"Action?"}
    action -->|APPROVE| grant["Gọi Identity Service cấp role seller"]
    grant --> grantOk{"Cấp role thành công?"}
    grantOk -->|Không| identityError["Trả lỗi Identity Service"]
    grantOk -->|Có| saveApproved["Lưu APPROVED"]
    action -->|REJECT| note["Kiểm tra review note"]
    note --> saveRejected["Lưu REJECTED"]
    action -->|SUSPEND| saveSuspended["Lưu SUSPENDED"]
    saveApproved --> publish["Publish seller-shop-status-changed"]
    saveRejected --> publish
    saveSuspended --> publish
    publish --> suspended{"SUSPENDED?"}
    suspended -->|Có| inactivate["Product Service inactivate product ACTIVE"]
    suspended -->|Không| done([Hoàn tất])
    inactivate --> done
    invalid --> done
    identityError --> done
```

## Evidence

- Seller/admin API: `seller-service/.../controller/SellerShopController.java`, `AdminSellerShopController.java`.
- State transition, role grant và publish: `seller-service/.../service/implement/SellerShopServiceImpl.java`, `messaging/SellerShopEventPublisher.java`.
- Product suspension reaction: `product-service/.../messaging/consumer/SellerShopEventConsumer.java`, `service/implement/ProductServiceImpl.java`.

## Chưa xác minh

- Consumer Product Service hiện chỉ có nhánh `SUSPENDED`; hành vi tự động re-activate product nếu shop được duyệt lại chưa được xác minh.
