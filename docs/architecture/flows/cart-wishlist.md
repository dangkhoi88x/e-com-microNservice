# Flow — Thêm product vào cart hoặc wishlist

## Scope

Cart Service và Wishlist Service đều lấy product/variant snapshot từ Product Service trước khi persist. Cart tự tạo active cart nếu chưa có, gộp item trùng và giữ selection/checkout lock. Wishlist dùng PostgreSQL `ON CONFLICT DO NOTHING` để idempotently tạo item theo user/product/variant.

![Sequence cart và wishlist](../diagrams/cart-wishlist-sequence.png)

```mermaid
sequenceDiagram
    actor User as Người dùng
    participant GW as API Gateway
    participant Cart as Cart Service
    participant Wish as Wishlist Service
    participant Product as Product Service
    participant CartDB as Cart database
    participant WishDB as Wishlist database

    alt Thêm cart
        User->>GW: POST /cart/api/v1/cart/items
        GW->>Cart: Forward REST/HTTPS + bearer token
        Cart->>Product: GET product/variant snapshot (REST)
        Product-->>Cart: ProductSnapshot
        Cart->>CartDB: Tạo active cart nếu chưa có
        Cart->>CartDB: Insert hoặc gộp CartItem
        Cart-->>User: 200 CartResponse
    else Thêm wishlist
        User->>GW: POST /wishlist/api/v1/wishlist/items
        GW->>Wish: Forward REST/HTTPS + bearer token
        Wish->>Product: GET product/variant snapshot (REST)
        Product-->>Wish: ProductSnapshot
        Wish->>WishDB: INSERT ON CONFLICT DO NOTHING
        Wish-->>User: 201 WishlistItemResponse
    end
```

## Activity: snapshot và persistence idempotent

![Activity cart và wishlist](../diagrams/cart-wishlist-activity.png)

```mermaid
flowchart TD
    start([Lưu product cho user]) --> target{"Cart hay wishlist?"}
    target -->|Cart| cartSnapshot["Lấy product/variant snapshot"]
    cartSnapshot --> activeCart["Tìm hoặc tạo active cart"]
    activeCart --> duplicateCart{"CartItem trùng?"}
    duplicateCart -->|Có| merge["Cộng quantity, refresh snapshot"]
    duplicateCart -->|Không| add["Tạo CartItem selected"]
    target -->|Wishlist| wishSnapshot["Lấy product/variant snapshot"]
    wishSnapshot --> insert["Insert wishlist với ON CONFLICT DO NOTHING"]
    merge --> result["Trả updated response"]
    add --> result
    insert --> result
    result --> done([Hoàn tất])
```

## Evidence

- Cart endpoints/business logic: `cart-service/.../controller/CartController.java`, `service/implement/CartServiceImpl.java`, `client/ProductClient.java`.
- Wishlist endpoints/business logic: `wishlist-service/.../controller/WishlistController.java`, `service/implement/WishlistServiceImpl.java`, `client/ProductClient.java`.

## Chưa xác minh

- Product snapshot read consistency/cache policy giữa Product, Cart và Wishlist cần kiểm tra integration test.
