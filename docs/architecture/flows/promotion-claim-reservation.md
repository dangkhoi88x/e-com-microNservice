# Flow — Claim promotion và reservation theo order

## Scope

Người dùng claim promotion campaign đang ACTIVE. Khi checkout, Order Service gọi internal promotion reserve. Promotion Service yêu cầu claim tồn tại, tạo `PromotionUsage` ở `RESERVED` với hạn 30 phút. Payment thành công dẫn đến confirm/`USED`; cancellation hoặc payment failure dẫn đến release/`RELEASED`.

![Sequence promotion claim và reservation](../diagrams/promotion-claim-reservation-sequence.png)

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant GW as API Gateway
    participant Promo as Promotion Service
    participant PromoDB as Promotion database
    participant Order as Order Service
    participant Kafka as Kafka

    User->>GW: POST /promotion/api/v1/promotions/claims/{campaignId}
    GW->>Promo: Forward REST/HTTPS
    Promo->>PromoDB: Kiểm tra campaign ACTIVE/time/usage limit
    Promo->>PromoDB: Lưu PromotionClaim nếu chưa có
    Promo-->>User: 200 claimed campaign
    Order->>Promo: POST /internal/promotions/reserve (REST)
    Promo->>PromoDB: Kiểm tra claim và tạo PromotionUsage RESERVED
    Promo-->>Order: Discount calculation
    Kafka-->>Order: Payment success hoặc cancellation event
    alt Payment thành công
        Order->>Promo: POST /internal/promotions/confirm (REST)
        Promo->>PromoDB: Chuyển USED và tăng usedCount
    else Payment thất bại hoặc order huỷ
        Order->>Promo: POST /internal/promotions/release (REST)
        Promo->>PromoDB: Chuyển RELEASED
    end
```

## Activity: eligibility và usage state

![Activity promotion claim và reservation](../diagrams/promotion-claim-reservation-activity.png)

```mermaid
flowchart TD
    start([Claim promotion]) --> active{"Campaign ACTIVE, đúng thời gian, còn quota?"}
    active -->|Không| campaignError["Trả promotion error"]
    active -->|Có| claim["Lưu PromotionClaim idempotent"]
    claim --> reserve["Checkout gọi reserve"]
    reserve --> hasClaim{"User đã claim?"}
    hasClaim -->|Không| notClaimed["Trả PROMOTION_NOT_CLAIMED"]
    hasClaim -->|Có| usage["Lưu PromotionUsage RESERVED, hết hạn 30 phút"]
    usage --> payment{"Payment/order result?"}
    payment -->|Success| used["Confirm USED và tăng usedCount"]
    payment -->|Failed/Cancelled| released["Release RESERVED thành RELEASED"]
    used --> done([Hoàn tất])
    released --> done
    campaignError --> done
    notClaimed --> done
```

## Evidence

- Claim API: `promotion-service/.../controller/PromotionClaimController.java`.
- Claim/reserve/confirm/release and limits: `promotion-service/.../service/implement/PromotionUsageServiceImpl.java`.
- Internal APIs: `promotion-service/.../controller/InternalPromotionController.java`.
- Caller flows: `order-service/.../service/implement/OrderServiceImpl.java`.

## Chưa xác minh

- Job tự động xử lý reservation hết hạn không được thấy trong service đã kiểm tra; chỉ expiry timestamp được set.
