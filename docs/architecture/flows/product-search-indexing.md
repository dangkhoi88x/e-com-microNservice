# Flow — Product command và search indexing

## Scope

Seller product command được Gateway route vào Product Service. `ProductServiceImpl` persist product, có thể tương tác Seller/Inventory client, và publish `product-created` hoặc `product-updated`. Search Service consume các topic này để cập nhật Elasticsearch document.

![Sequence product và search indexing](../diagrams/product-search-indexing-sequence.png)

```mermaid
sequenceDiagram
    actor Seller as Người bán
    participant UI as web-app
    participant GW as API Gateway
    participant Product as Product Service
    participant SellerSvc as Seller Service
    participant Inventory as Inventory Service
    participant ProductDB as Product PostgreSQL
    participant Kafka as Kafka
    participant Search as Search Service
    participant ES as Elasticsearch

    Seller->>UI: Tạo/cập nhật product hoặc variant
    UI->>GW: POST /api/v1/seller/products
    GW->>Product: Forward REST/HTTPS + JWT
    Product->>SellerSvc: Verify seller eligibility khi cần (REST)
    SellerSvc-->>Product: Eligibility result
    Product->>ProductDB: Persist product/variant (JPA/JDBC)
    opt Có quantity trong command
        Product->>Inventory: Set/query available quantity (REST)
        Inventory-->>Product: Inventory response
    end
    Product->>Kafka: Publish product-created hoặc product-updated
    Kafka-->>Search: Deliver product lifecycle event
    Search->>ES: Save/delete ProductDocument
    ES-->>Search: Index result
```

## Activity: event-driven read model

![Activity product read model](../diagrams/product-search-indexing-activity.png)

```mermaid
flowchart TD
    start([Product command hoặc inventory update]) --> persist["Product Service persist/update catalog read model"]
    persist --> event{"Loại event"}
    event -->|Create| created["Publish product-created"]
    event -->|Update hoặc inventory sync| updated["Publish product-updated"]
    event -->|Delete| deleted["Publish product-deleted"]
    created --> consume["Search ProductEventConsumer consume Kafka event"]
    updated --> consume
    deleted --> remove["Delete ProductDocument khỏi Elasticsearch"]
    consume --> index["Save ProductDocument vào Elasticsearch"]
    index --> finish([Search query dùng read model])
    remove --> finish
```

## Evidence

- Seller product endpoint: `product-service/src/main/java/com/example/productservice/controller/SellerProductController.java`.
- Product query endpoint: `product-service/src/main/java/com/example/productservice/controller/ProductController.java`.
- Product persist/event: `product-service/src/main/java/com/example/productservice/service/implement/ProductServiceImpl.java`.
- Seller/Inventory calls: `product-service/src/main/java/com/example/productservice/client/SellerClient.java`, `InventoryClient.java`.
- Search Kafka consumer: `search-service/src/main/java/com/example/searchservice/messaging/ProductEventConsumer.java`.

## Chưa xác minh

Không có transaction/outbox pattern được xác nhận giữa product database commit và `KafkaTemplate.send` trong phạm vi flow này. Cần integration test/runtime observation để đánh giá khả năng retry và eventual consistency khi publish event lỗi.
