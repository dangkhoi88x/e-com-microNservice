# Flow — Upload và truy cập media asset

## Scope

Media Service nhận ảnh multipart. Với `PRODUCT_IMAGE`, controller yêu cầu role seller từ JWT. Service kiểm tra bucket configuration, giới hạn kích thước, magic header JPEG/PNG/WEBP và content type; sau đó upload object vào S3-compatible storage, lưu metadata. Nếu lưu metadata thất bại, service cố gắng xoá object đã upload. Download URL là presigned URL; endpoint content trả redirect tới URL đó.

![Sequence upload media](../diagrams/media-upload-sequence.png)

```mermaid
sequenceDiagram
    actor User as Người dùng hoặc seller
    participant GW as API Gateway
    participant Media as Media Service
    participant S3 as S3-compatible storage
    participant MediaDB as Media database

    User->>GW: POST /media/api/v1/media/images multipart/form-data
    GW->>Media: Forward REST/HTTPS + JWT
    Media->>Media: Kiểm tra purpose, size, header và content type
    Media->>S3: PutObject S3 API
    S3-->>Media: Upload thành công
    Media->>MediaDB: Lưu MediaAsset metadata
    alt Lưu metadata thất bại
        Media->>S3: DeleteObject compensation
    else Thành công
        Media-->>User: 201 MediaResponse
    end
    User->>GW: GET /media/api/v1/media/{id}/content
    GW->>Media: Forward REST/HTTPS
    Media->>S3: Tạo presigned GetObject URL
    Media-->>User: 302 Location tới object URL
```

## Activity: validation và compensation upload

![Activity validation media upload](../diagrams/media-upload-activity.png)

```mermaid
flowchart TD
    start([Upload image]) --> purpose{"PRODUCT_IMAGE và user có role seller?"}
    purpose -->|Không| denied["Trả MEDIA_ACCESS_DENIED"]
    purpose -->|Có hoặc public purpose| configured{"S3 bucket được cấu hình?"}
    configured -->|Không| configError["Trả S3_NOT_CONFIGURED"]
    configured -->|Có| valid{"File size, header, content type hợp lệ?"}
    valid -->|Không| fileError["Trả INVALID_MEDIA_FILE"]
    valid -->|Có| upload["PutObject vào S3-compatible storage"]
    upload --> stored{"Lưu metadata thành công?"}
    stored -->|Có| response["Trả MediaResponse và presigned URL"]
    stored -->|Không| cleanup["Cố gắng DeleteObject compensation"]
    cleanup --> dbError["Trả lỗi persistence"]
    denied --> done([Kết thúc])
    configError --> done
    fileError --> done
    response --> done
    dbError --> done
```

## Evidence

- Upload/download/content API và seller guard: `media-service/.../controller/MediaController.java`.
- Validation, S3 put/delete, metadata persistence và compensation: `media-service/.../service/MediaAssetService.java`.

## Chưa xác minh

- Bucket name, endpoint, access key, presigned URL TTL và policy là runtime secret/configuration, nên không được ghi giá trị trong tài liệu.
- Quét virus/CDN/object lifecycle không thấy trong code đã kiểm tra.
