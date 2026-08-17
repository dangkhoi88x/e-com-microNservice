# Flow — Product review và đồng bộ review summary vào search

## Scope

Người dùng tạo review cho order item. Review Service kiểm tra eligibility qua Order Service, lấy tên public qua Profile Service, lưu review trạng thái `PUBLISHED`, tính review summary và chỉ publish `review-summary-changed` sau DB commit. Search Service nhận event để cập nhật average rating/review count trong Elasticsearch và xóa cache aggregate/suggestion liên quan.

![Sequence review summary và search](../diagrams/review-summary-search-sequence.png)

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant GW as API Gateway
    participant Review as Review Service
    participant Order as Order Service
    participant Profile as Profile Service
    participant ReviewDB as Review database
    participant Kafka as Kafka
    participant Search as Search Service
    participant ES as Elasticsearch
    participant Redis as Redis cache

    User->>GW: POST /review/api/v1/reviews
    GW->>Review: Forward REST/HTTPS + bearer token
    Review->>Order: Check order item eligibility (REST)
    Order-->>Review: eligible / ineligible
    Review->>Profile: Get public reviewer name (REST)
    Profile-->>Review: reviewer name
    Review->>ReviewDB: Lưu ProductReview PUBLISHED
    Review->>ReviewDB: Tính averageRating và reviewCount
    Review->>Kafka: Publish review-summary-changed sau commit
    Kafka-->>Search: Deliver ReviewSummaryChangedEvent
    Search->>ES: Cập nhật summary product document
    Search->>Redis: Evict cache aggregation/suggestion
    Review-->>User: 201 Created
```

## Activity: điều kiện tạo review

![Activity kiểm tra review](../diagrams/review-summary-search-activity.png)

```mermaid
flowchart TD
    start([Tạo review]) --> duplicate{"Đã có review cho order item?"}
    duplicate -->|Có| duplicateError["Trả REVIEW_ALREADY_EXISTS"]
    duplicate -->|Không| eligibility["Gọi Order Service kiểm tra eligibility"]
    eligibility --> allowed{"Có thể review?"}
    allowed -->|Không| denied["Trả REVIEW_NOT_ELIGIBLE"]
    allowed -->|Có| profile["Lấy public reviewer name"]
    profile --> save["Lưu review PUBLISHED"]
    save --> summarize["Tính average rating và review count"]
    summarize --> publish["Publish review-summary-changed sau commit"]
    publish --> index["Search cập nhật Elasticsearch và xoá cache"]
    index --> done([Hoàn tất])
    duplicateError --> done
    denied --> done
```

## Evidence

- Review endpoint và authorization: `review-service/.../controller/ReviewController.java`.
- Eligibility, profile lookup và summary: `review-service/.../service/implement/ReviewServiceImpl.java`.
- Publish after commit: `review-service/.../messaging/ReviewSummaryEventPublisher.java`.
- Kafka consumer/index update: `search-service/.../messaging/ReviewSummaryEventConsumer.java`, `service/impl/ProductDocumentServiceImpl.java`.

## Chưa xác minh

- Error mapping/retry khi Order hoặc Profile Service không phản hồi cần kiểm tra client configuration và test integration.
