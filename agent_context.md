# Agent Context — NovaShop Microservices

This file is operational context for AI coding agents working in this repository. Read it before changing code. For human onboarding and detailed startup instructions, also read `start_here.md`.

## 1. Mission and repository scope

This repository implements a personal e-commerce platform named NovaShop with:

- A customer storefront.
- An admin workspace.
- Spring Boot microservices.
- React/Vite frontend.
- PostgreSQL, MongoDB, Redis, Kafka and Elasticsearch infrastructure.
- Eureka discovery and an API Gateway.

The workspace root is an aggregate repository. Each Spring service remains independently deployable and owns its own business logic.

## 2. First actions for every task

Before editing:

1. Read the user request and identify the owning service.
2. Run `git status --short` and preserve unrelated user changes.
3. Search with `rg`/`rg --files` before assuming a class, endpoint or DTO is missing.
4. Inspect the relevant controller, service interface, implementation, repository, mapper, security configuration and frontend client together.
5. Check whether the same behavior already exists through REST, Kafka or an internal endpoint.
6. Make the smallest coherent cross-service change.
7. Build/test every touched module in proportion to risk.

Do not overwrite, reset, delete or reformat unrelated work. Do not assume a dirty working tree belongs to the agent.

## 3. Repository map

| Module | Ownership |
| --- | --- |
| `Microservice-ecom` | Identity, login, registration, JWT, refresh token, token introspection |
| `api-gateway-service` | Public routing, CORS, JWT/introspection gateway filter |
| `discovery-server` | Eureka registry |
| `profile-service` | Customer profile |
| `notification-service` | In-app/email notification consumers |
| `product-service` | Category, product, images, options, option values and variants |
| `inventory-service` | Available/reserved/sold stock and inventory reservations |
| `cart-service` | Cart, cart items, selected state and checkout locking |
| `wishlist-service` | Per-user product/variant wishlist |
| `search-service` | Elasticsearch product read model, filtering and autocomplete |
| `order-service` | Checkout orchestration and order lifecycle |
| `payment-service` | Payment lifecycle and payment events |
| `promotion-service` | Campaigns, claims, usage reservations, flash deals |
| `web-app` | Customer storefront and admin UI |
| `database` | Demo product/inventory SQL seeds |
| `bruno` | API test collections, currently focused on Promotion integration |

## 4. Runtime map

| Component | Local port |
| --- | ---: |
| Profile | 8081 |
| Notification | 8083 |
| Product | 8084 |
| Order | 8086 |
| Inventory | 8087 |
| Payment | 8088 |
| Cart | 8089 |
| Identity HTTP | 8090 |
| Identity gRPC | 9090 |
| Wishlist | 8092 |
| Search | 8093 |
| Promotion via IntelliJ | 8095 |
| Promotion Docker host mapping | 8095 -> container 8095 (must stay 1:1) |
| Eureka | 8761 |
| API Gateway | 9191 |
| Frontend | 5173 |
| Kafka UI | 8085 |
| Elasticsearch | 9200 |

Recommended local mode:

- Docker: databases, Redis, MongoDB, Kafka, Elasticsearch.
- IntelliJ/Maven: Spring application services.
- Never run the same service in Docker and IntelliJ at the same time.

Promotion is especially sensitive to duplicate instances. Two Promotion registrations in Eureka make Gateway behavior intermittent. When Promotion runs in IntelliJ, stop only its application container and keep its database:

```powershell
docker compose stop promotion-service
docker compose up -d promotion-postgres
```

## 5. Current data ownership

| Storage | Current owner/use |
| --- | --- |
| PostgreSQL `5432/postgres` | Product, Cart, Order, Payment, Wishlist currently share it |
| PostgreSQL `5433/identity-service` | Identity |
| PostgreSQL `5435/inventory_db` | Inventory |
| PostgreSQL `5437/promotion_db` | Promotion |
| PostgreSQL `5438/shipping_db` | Shipping |
| MongoDB `profile-service` | Profile |
| MongoDB `notification-service` | Notification |
| Redis `6379` | Identity refresh/session support |
| Elasticsearch `9200` | Search read model |

Architectural target is database-per-service even though several services currently share one local PostgreSQL database. Never access another service's repository/entity directly. Cross-service access must use an API or event.

## 6. Architecture invariants

### Identity and authorization

- The authenticated user identity comes from the JWT subject/claims.
- Do not trust `userId` supplied by public request bodies or query parameters.
- Gateway calls Identity gRPC on port 9090 for token introspection.
- Frontend uses an access token plus refresh-token cookie.
- Axios must keep `withCredentials: true` for refresh flow.
- A 401 should trigger at most one shared refresh request and retry the original request once.
- Admin frontend routes currently require authentication only; role-based ADMIN guarding is intentionally deferred.

### Product and variant

- `Product` represents catalog-level information.
- `ProductVariant` represents a purchasable SKU.
- `ProductOption` and `ProductOptionValue` define valid option metadata such as Color, Size and Storage.
- Variant `attributes` may only contain values defined by the product's options.
- Order, Cart, Wishlist and Inventory should retain `variantId` whenever a product has variants.
- Product images/options/variants returned to clients should be mapped through response DTOs, not exposed as JPA entities.

### Inventory

Inventory Service is the stock source of truth.

- Reserve: `availableQuantity -= quantity`, `reservedQuantity += quantity`.
- Confirm payment: `reservedQuantity -= quantity`, `soldQuantity += quantity`; available stays unchanged.
- Release: `reservedQuantity -= quantity`, `availableQuantity += quantity`.
- Reservation transitions are `PENDING -> CONFIRMED` or `PENDING -> RELEASED`.
- Repeated confirm/release calls must be no-ops after the first valid transition.
- `Product.quantity` and Search `inStock` are denormalized read data, not checkout authority.

### Cart and checkout locking

- Checkout starts from selected CartItems.
- After an order is created/reserved, only those exact items receive `checkoutOrderId = orderId`.
- Do not delete every `selected=true` item after payment; the user may add/select new items while payment is in progress.
- Payment success finalizes only items matching the order's `checkoutOrderId`.
- Payment failed/cancelled clears only that order's `checkoutOrderId` so the items can be paid again.
- Internal finalize/release endpoints must remain idempotent.

### Order correlation

- Internal service correlation currently uses the canonical `orderId` UUID.
- `orderCode` may be displayed to users, but do not replace internal `orderId` correlation unless explicitly redesigning every service/event.
- Order stores immutable purchase snapshots: product name, variant, unit price, quantity and subtotal at checkout time.

### Promotion and flash deal

- Campaign lifecycle: `DRAFT`, `ACTIVE`, `INACTIVE`, `EXPIRED`.
- Usage lifecycle: `RESERVED`, `USED`, `RELEASED`.
- Flash deal lifecycle: `DRAFT`, `SCHEDULED`, `LIVE`, `ENDED`, `SOLD_OUT`.
- Checkout validates and reserves promotions before entering a stable payment state.
- Payment success confirms usage; failed/cancelled payment releases it.
- Confirm/release must be idempotent by order.
- Never compute the authoritative discount only in the frontend.

### Payment and event handling

- Payment lifecycle: `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`.
- Payment Service publishes `payment-success`, `payment-failed` and `payment-cancelled`.
- Order Service reacts by updating the order and coordinating Promotion, Flash Deal, Inventory and Cart.
- Consumers and internal endpoints must tolerate duplicate Kafka delivery.
- A repeated success event must not increment sold stock or promotion usage twice.

### Search

- Elasticsearch is a read model, not transactional authority.
- Product create/update/delete events maintain the index.
- Inventory changes propagate through product/search synchronization.
- Storefront search, filter, aggregation and autocomplete should use Search Service.
- Checkout must re-read authoritative Product/Variant and Inventory data.

### Wishlist

- Wishlist belongs to the authenticated user.
- Backend validates product and variant with Product Service.
- Product name, price and image snapshots must come from Product Service, not untrusted frontend payload.
- Adding the same user/product/variant must be idempotent.
- Frontend optimistic heart changes must roll back when the backend mutation fails.

## 7. Checkout transaction flow

```text
selected CartItems
-> Order checkout endpoint
-> Product/Variant snapshot
-> Promotion/Flash Deal validation
-> create Order PENDING
-> Inventory reserve
-> Promotion/Flash Deal reserve
-> Order PENDING_PAYMENT
-> mark CartItems with checkoutOrderId
-> create Payment PENDING
```

Success:

```text
Payment SUCCESS event
-> Order CONFIRMED
-> Inventory confirm
-> Promotion/Flash Deal confirm
-> Cart finalize exact checkoutOrderId items
```

Failure/cancellation:

```text
Payment FAILED/CANCELLED event
-> release Inventory
-> release Promotion/Flash Deal
-> release Cart checkoutOrderId
-> items remain available for retry
```

When adding a step to this flow, define its compensation step before considering the feature complete.

## 8. API conventions

Public API responses generally use an `ApiResponse` wrapper containing:

```text
status
message
data
```

Errors should use the service's standard stack:

```text
exception/ErrorCode
exception/<ServiceName>Exception
exception/ErrorResponse
exception/GlobalExceptionHandler
```

Avoid ad-hoc `ResponseStatusException` when the module already uses standardized error handling.

Use Bean Validation on request DTOs. Keep request and response DTOs separate. Use MapStruct mapper classes under a `mapper` package when mapping becomes non-trivial.

Internal service endpoints currently include:

```text
POST /internal/inventory/confirm
POST /internal/inventory/release
POST /internal/cart/{orderId}/finalize
POST /internal/cart/{orderId}/release
POST /internal/promotions/validate
POST /internal/promotions/reserve
POST /internal/promotions/confirm
POST /internal/promotions/release
POST /internal/flash-deals/reserve
POST /internal/flash-deals/confirm
POST /internal/flash-deals/release
```

Do not expose internal endpoints directly in storefront code.

## 9. API Gateway context

All gateway routes live in `api-gateway-service/src/main/java/.../GatewayConfiguration.java`. Add new routes there only; `application.yaml` no longer declares any.

They used to be split across both files, which duplicated four paths. The copies were not equivalent — the YAML `/order/**` route lacked `stripPrefix(1)` — so which definition won decided whether ordering worked.

Gateway prefixes in current use:

```text
/identity/**
/profile/**
/notification/**
/product/**
/inventory/**
/order/**
/payment/**
/search/**
/api/v1/cart/**
/api/v1/wishlist/**
/api/v1/search/**
/api/v1/promotions/**
/api/v1/flash-deals/**
```

Frontend API base URL is `http://localhost:9191` in `web-app/src/configurations/configuration.js`.

## 10. Frontend context

`web-app` contains storefront and admin code in one React application.

Important areas:

```text
web-app/src/pages/                 # Page-level UI
web-app/src/components/            # Shared/admin/storefront components
web-app/src/components/ui/         # shadcn-style primitives
web-app/src/services/              # HTTP clients per backend domain
web-app/src/configurations/        # API paths and Axios interceptors
web-app/src/routes/AppRoutes.jsx   # All routes and current auth guarding
```

UI principles already established:

- Storefront branding: NovaShop, white + baby blue + deep blue.
- Admin uses MUI/shadcn-inspired components and larger readable table typography.
- Product cards should have equal heights and aligned actions.
- Product navigation uses slug routes.
- “Add to cart” mutates the cart and should not automatically navigate unless the CTA explicitly says checkout/view cart.
- Shared storefront pages should use `ShopStoreHeader`/`StorefrontLayout` consistently.
- Buttons and clickable cards must show pointer cursor and visible hover/focus states.
- Do not mix customer profile routes with admin `/profile`.

When changing a backend DTO, inspect every corresponding frontend service and page before finishing.

## 11. Build and verification commands

PowerShell examples from repository root:

```powershell
mvn -DskipTests package
mvn -f promotion-service\pom.xml -DskipTests package
mvn -f order-service\pom.xml -DskipTests package
```

Root Maven aggregates all seventeen services, so `mvn -DskipTests package` from the repository root builds everything. CI runs exactly that.

Frontend:

```powershell
cd web-app
npm ci
npm run lint
npm run build
```

Run a service with its wrapper:

```powershell
cd order-service
.\mvnw.cmd spring-boot:run
```

Identity and Profile currently rely on installed Maven because no wrapper is present in those modules:

```powershell
mvn -f Microservice-ecom\pom.xml spring-boot:run
mvn -f profile-service\pom.xml spring-boot:run
```

Useful read-only checks:

```powershell
git status --short
docker compose ps
Get-NetTCPConnection -LocalPort 9191 -State Listen
curl.exe http://localhost:8761
curl.exe http://localhost:9200/_cluster/health
```

Do not treat Vite's chunk-size warning as a build failure. Do treat compiler errors, ESLint errors and failed integration behavior as incomplete work.

## 12. Seed and API testing

PowerShell seed commands:

```powershell
Get-Content .\database\seed-products.sql -Raw | docker exec -i product-postgres psql -U root -d postgres
Get-Content .\database\seed-inventory.sql -Raw | docker exec -i inventory-postgres psql -U postgres -d inventory_db
```

Run product seed before inventory seed.

Promotion Bruno collection:

```text
bruno/promotion-service/
```

Correct login through Gateway:

```text
POST http://localhost:9191/identity/auth/login
```

Bruno does not automatically share the browser's access token or refresh cookie.

## 13. Known technical debt and traps

1. `promotion-service` is missing from root Maven modules; `shipping-service` has already been added.
2. Gateway routes are duplicated across Java and YAML configuration.
3. Several services share one PostgreSQL database.
4. Application Docker containers can advertise hostnames/ports that a host-run Gateway cannot reach.
5. Running Docker and IntelliJ instances simultaneously creates duplicate Eureka registrations.
6. Some local configuration files contain credentials/secrets. Never copy them into output or new docs; migrate to environment variables.
7. Many services use `ddl-auto:update`; production should use Flyway/Liquibase.
8. Spring Boot versions differ across modules (`4.0.6` and `4.1.0`). Verify compatibility before centralizing dependency management.
9. Admin route role authorization is intentionally not complete.
10. Frontend bundle is large and should eventually use route-level lazy loading.
11. Legacy documentation/output may display mojibake if read with the wrong encoding. New Markdown files must remain UTF-8.

## 14. Security constraints

- Never reveal values from `.env*`, JWT secrets, SMTP passwords or Elasticsearch API keys.
- Do not add credentials to Markdown examples.
- Preserve refresh-token cookie settings and CORS origin constraints.
- Validate ownership in each service; Gateway authentication alone is not object-level authorization.
- Internal APIs need a future service-to-service authentication strategy even if currently permitted for local development.
- Do not hardcode a user/admin identity to make a test pass.

## 15. Definition of done

A code change is complete only when relevant items below are satisfied:

- The owning service and cross-service contracts are correct.
- Request validation and standardized errors are implemented.
- Security/ownership is preserved.
- Idempotency is preserved for events and internal checkout operations.
- Compensation behavior exists for checkout reservation failures.
- Mapper/DTO/repository/service/controller layers remain separated.
- Gateway route is present when public access is required.
- Frontend API client and UI states are updated.
- Loading, empty, success and error states are handled.
- Touched backend modules build or tests pass.
- Frontend builds when frontend code changed.
- No unrelated user changes are overwritten.
- No secret is introduced into code, logs or documentation.

## 16. Where to start by task type

| Task | Start here |
| --- | --- |
| Login/JWT/refresh | `Microservice-ecom`, `api-gateway-service`, frontend `httpClient.js` |
| Product/category/variant | `product-service` and frontend product forms/pages |
| Stock/reservation | `inventory-service` |
| Cart behavior | `cart-service`, frontend Cart/storefront card components |
| Checkout/order | `order-service`, Cart/Inventory/Promotion clients |
| Payment transition | `payment-service`, payment event consumers in Order/Inventory |
| Voucher/campaign/flash sale | `promotion-service`, admin Promotions/FlashDeals pages |
| Search/filter/autocomplete | `search-service`, frontend ShopSearch/header/category pages |
| Wishlist | `wishlist-service`, `ShopWishlist`, shared product cards/detail |
| Notification | `notification-service`, notification consumers/pages |
| Gateway 404/503 | Gateway route sources, Eureka registration and advertised instance address |
| Intermittent service errors | Check duplicate Eureka instances before adding frontend retries |

## 17. Agent handoff format

At the end of a task, report:

1. Outcome first.
2. Files materially changed.
3. Behavior/business rule implemented.
4. Verification commands and their result.
5. Any remaining blocker or required service restart.

Do not claim success only because code was edited. Report build/test limitations explicitly.
