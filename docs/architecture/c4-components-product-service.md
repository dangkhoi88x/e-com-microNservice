# C4 Component — Product Service

Product Service sở hữu catalog/product variant. Nó đồng bộ tồn kho với Inventory Service và publish product lifecycle events cho Search Service cập nhật read model.

![Component diagram Product Service](diagrams/c4-components-product-service.png)

```mermaid
C4Component
  title Component diagram — Product Service

  Container_Ext(gateway, "API Gateway", "Spring Cloud Gateway", "Route product API")
  Container_Ext(seller, "Seller Service", "Spring Boot", "Seller eligibility")
  Container_Ext(inventory, "Inventory Service", "Spring Boot", "Available quantity")
  ContainerQueue(kafka, "Kafka", "Apache Kafka", "Product lifecycle events")
  ContainerDb(productDb, "Product persistence", "PostgreSQL", "Catalog and variants")
  ContainerDb(redis, "Redis", "Redis", "Cache support")

  Component(publicController, "ProductController", "Spring MVC", "Public catalog read API")
  Component(sellerController, "SellerProductController", "Spring MVC", "Seller product/variant commands")
  Component(productService, "ProductServiceImpl", "Spring service", "Catalog business logic and event publishing")
  Component(productRepo, "ProductRepository and variant repositories", "Spring Data JPA", "Persist catalog")
  Component(sellerClient, "SellerClient", "RestClient", "Verify seller eligibility")
  Component(inventoryClient, "InventoryClient", "RestClient", "Set/query inventory quantity")
  Component(inventoryConsumer, "InventoryEventConsumer", "Kafka listener", "Sync inventory read model")

  Rel(gateway, publicController, "Forward public product request", "HTTPS/JSON")
  Rel(gateway, sellerController, "Forward seller command", "HTTPS/JSON")
  Rel(publicController, productService, "Query catalog", "Java")
  Rel(sellerController, productService, "Create/update product", "Java")
  Rel(productService, sellerClient, "Check seller", "REST/HTTPS")
  Rel(sellerClient, seller, "Get eligibility", "REST/HTTPS")
  Rel(productService, inventoryClient, "Set/query available quantity", "REST/HTTPS")
  Rel(inventoryClient, inventory, "Call inventory API", "REST/HTTPS")
  Rel(productService, productRepo, "Persist catalog", "JPA")
  Rel(productRepo, productDb, "Persist", "JDBC")
  Rel(productService, kafka, "Publish product-created/product-updated", "Kafka")
  Rel(kafka, inventoryConsumer, "Deliver inventory-updated", "Kafka")
  Rel(inventoryConsumer, productService, "Sync quantity", "Java")
  Rel(productService, redis, "Cache support", "Redis")
```

## Evidence

- Public API: `product-service/src/main/java/com/example/productservice/controller/ProductController.java`.
- Seller API: `product-service/src/main/java/com/example/productservice/controller/SellerProductController.java`.
- Business/event code: `product-service/src/main/java/com/example/productservice/service/implement/ProductServiceImpl.java`.
- Inter-service clients: `product-service/src/main/java/com/example/productservice/client/SellerClient.java` và `InventoryClient.java`.
- Inventory consumer: `product-service/src/main/java/com/example/productservice/messaging/consumer/InventoryEventConsumer.java`.
