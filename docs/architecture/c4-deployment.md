# Local deployment view

Đây là deployment view cho local development dựa trên `docker-compose.yaml`, Dockerfiles hiện có và `application.yaml`. Nó không đại diện cho production topology.

![Local deployment view](diagrams/c4-deployment.png)

```mermaid
flowchart TB
    browser["Trình duyệt"] --> frontend["web-app\nReact + Vite"]
    frontend --> gateway["api-gateway-service\nSpring Cloud Gateway :9191"]
    gateway --> eureka["discovery-server\nEureka :8761"]
    gateway --> services["Spring Boot service processes\nchạy local qua Maven/IDE"]

    subgraph docker["Docker Compose: microservice_network"]
        postgres["PostgreSQL containers"]
        mongo["MongoDB"]
        redis["Redis"]
        kafka["Kafka + Kafka UI"]
        elastic["Elasticsearch"]
        elk["Logstash + Kibana"]
        selectedApps["Một số app containers\nreview, shipping, seller, promotion, wishlist"]
    end

    services --> postgres
    services --> mongo
    services --> redis
    services --> kafka
    services --> elastic
    services --> elk
    selectedApps --> postgres
    selectedApps --> kafka
    selectedApps --> eureka
```

## Cách đọc topology

- Root Compose khởi tạo Kafka, Redis, MongoDB, Elasticsearch, Logstash, Kibana và PostgreSQL containers cho nhiều bounded context.
- `review-service`, `shipping-service`, `seller-service`, `promotion-service` và `wishlist-service` có application service definition trong root Compose. Những service khác có thể chạy từ Maven/IDE theo layout hiện tại.
- `media-service` và `review-service` cũng có Compose file riêng cho database local.
- API Gateway dùng Eureka `lb://...` routes. Để gọi qua Gateway, Discovery và target service cần chạy/đăng ký.
- `media-service` port runtime chưa xác minh từ `application.yaml`; không đoán port trong sơ đồ.

## Observability

Compose định nghĩa Elasticsearch, Logstash và Kibana. Đây là bằng chứng infrastructure có mặt; việc mọi service đã gửi log/trace thành công đến ELK cần xác minh bằng runtime logs.

## Evidence

- `docker-compose.yaml`
- `media-service/docker-compose.yaml`
- `review-service/docker-compose.yaml`
- `elk/logstash/pipeline/logstash.conf`
- `api-gateway-service/src/main/resources/application.yaml`
- `api-gateway-service/src/main/java/com/example/apigatewayservice/configuration/GatewayConfiguration.java`
