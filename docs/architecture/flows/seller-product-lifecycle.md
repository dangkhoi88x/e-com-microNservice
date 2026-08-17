# Flow — Seller product draft, moderation và search indexing

## Scope

Seller tạo product draft khi Seller Service xác nhận shop `APPROVED`. Product Service lưu product/variant ở `DRAFT`, đồng bộ quantity với Inventory Service và phát event product. Seller submit product thành `PENDING_APPROVAL`; admin approve/reject/hide qua Product Service. Mỗi thay đổi phát event để Search Service cập nhật Elasticsearch read model.

![Sequence seller product lifecycle](../diagrams/seller-product-lifecycle-sequence.png)

```mermaid
sequenceDiagram
    actor Seller as Người bán
    actor Admin as Quản trị viên
    participant GW as API Gateway
    participant Product as Product Service
    participant SellerSvc as Seller Service
    participant Inventory as Inventory Service
    participant ProductDB as Product database
    participant Kafka as Kafka
    participant Search as Search Service
    participant ES as Elasticsearch

    Seller->>GW: POST /product/api/v1/seller/products
    GW->>Product: Forward REST/HTTPS + bearer token
    Product->>SellerSvc: GET eligibility (REST)
    SellerSvc-->>Product: APPROVED shopId
    Product->>ProductDB: Lưu product/variants DRAFT
    Product->>Inventory: PUT available quantity (REST)
    Product->>Kafka: Publish product-created
    Kafka-->>Search: Deliver product-created
    Search->>ES: Index product document
    Seller->>GW: POST /product/api/v1/seller/products/{id}/submit
    GW->>Product: Forward REST/HTTPS
    Product->>ProductDB: Chuyển PENDING_APPROVAL
    Admin->>GW: PUT /product/api/v1/admin/products/{id}/review
    GW->>Product: Approve / reject / hide
    Product->>ProductDB: Lưu trạng thái moderation
    Product->>Kafka: Publish product-updated
    Kafka-->>Search: Deliver product-updated
    Search->>ES: Update product document
```

## Activity: product state transition

![Activity seller product lifecycle](../diagrams/seller-product-lifecycle-activity.png)

```mermaid
flowchart TD
    start([Seller tạo product]) --> eligibility{"Shop APPROVED?"}
    eligibility -->|Không| sellerError["Trả SELLER_NOT_ELIGIBLE"]
    eligibility -->|Có| draft["Lưu product và variants DRAFT"]
    draft --> inventory["Đồng bộ quantity sang Inventory"]
    inventory --> created["Publish product-created"]
    created --> submit["Seller submit product"]
    submit --> validDraft{"DRAFT hoặc REJECTED?"}
    validDraft -->|Không| transitionError["Trả INVALID_PRODUCT_TRANSITION"]
    validDraft -->|Có| pending["Lưu PENDING_APPROVAL"]
    pending --> review{"Admin action?"}
    review -->|APPROVE| active["Lưu ACTIVE"]
    review -->|REJECT| rejected["Lưu REJECTED kèm note"]
    review -->|HIDE| inactive["Lưu INACTIVE kèm note"]
    active --> updated["Publish product-updated và index search"]
    rejected --> updated
    inactive --> updated
    updated --> done([Hoàn tất])
    sellerError --> done
    transitionError --> done
```

## Evidence

- Seller endpoint: `product-service/.../controller/SellerProductController.java`.
- Product states, eligibility, inventory sync và event: `product-service/.../service/implement/ProductServiceImpl.java`.
- Admin moderation: `product-service/.../controller/AdminProductModerationController.java`.
- Search consumer: `search-service/.../messaging/ProductEventConsumer.java`.

## Chưa xác minh

- Retry/circuit-breaker parameters khi gọi Seller/Inventory Service cần đối chiếu runtime configuration.
