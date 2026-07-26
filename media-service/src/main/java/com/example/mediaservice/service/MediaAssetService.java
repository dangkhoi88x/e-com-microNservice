package com.example.mediaservice.service;

import com.example.mediaservice.configuration.S3Properties;
import com.example.mediaservice.configuration.MediaProperties;
import com.example.mediaservice.dto.MediaResponse;
import com.example.mediaservice.entity.MediaAsset;
import com.example.mediaservice.entity.MediaPurpose;
import com.example.mediaservice.exception.ErrorCode;
import com.example.mediaservice.exception.MediaServiceException;
import com.example.mediaservice.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaAssetService {

    private final MediaAssetRepository repository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;
    private final MediaProperties mediaProperties;

    public MediaResponse uploadImage(String ownerId, MediaPurpose purpose, MultipartFile file) {
        ensureStorageConfigured();
        ImageFormat format = validateAndDetectImage(file);
        String objectKey = "media/%s/%s/%s.%s".formatted(
                purpose.name().toLowerCase(Locale.ROOT), ownerId, UUID.randomUUID(), format.extension());

        try (InputStream input = file.getInputStream()) {
            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(objectKey)
                            .contentType(format.contentType())
                            .build(),
                    RequestBody.fromInputStream(input, file.getSize()));
        } catch (IOException | S3Exception exception) {
            throw new MediaServiceException(ErrorCode.MEDIA_STORAGE_UNAVAILABLE);
        }

        MediaAsset asset = new MediaAsset();
        asset.setOwnerId(ownerId);
        asset.setPurpose(purpose);
        asset.setObjectKey(objectKey);
        asset.setContentType(format.contentType());
        asset.setSizeBytes(file.getSize());
        try {
            return response(repository.save(asset), true);
        } catch (RuntimeException exception) {
            deleteObjectQuietly(objectKey);
            throw exception;
        }
    }

    public MediaResponse getDownloadUrl(UUID mediaId, String requesterId) {
        ensureStorageConfigured();
        MediaAsset asset = find(mediaId);
        boolean isOwner = requesterId != null && requesterId.equals(asset.getOwnerId());
        if (!asset.getPurpose().isPublic() && !isOwner) {
            throw new MediaServiceException(ErrorCode.MEDIA_ACCESS_DENIED);
        }
        return response(asset, asset.getPurpose().isPublic() || isOwner);
    }

    public void delete(UUID mediaId, String ownerId) {
        ensureStorageConfigured();
        MediaAsset asset = find(mediaId);
        if (!ownerId.equals(asset.getOwnerId())) {
            throw new MediaServiceException(ErrorCode.MEDIA_ACCESS_DENIED);
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket()).key(asset.getObjectKey()).build());
            repository.delete(asset);
        } catch (S3Exception exception) {
            throw new MediaServiceException(ErrorCode.MEDIA_STORAGE_UNAVAILABLE);
        }
    }

    public String getContentUrl(UUID mediaId, String requesterId) {
        return getDownloadUrl(mediaId, requesterId).downloadUrl();
    }

    private MediaAsset find(UUID mediaId) {
        return repository.findById(mediaId).orElseThrow(() -> new MediaServiceException(ErrorCode.MEDIA_NOT_FOUND));
    }

    private MediaResponse response(MediaAsset asset, boolean includeUrl) {
        String contentUrl = "%s/%s/content".formatted(mediaProperties.publicBaseUrl().replaceAll("/+$", ""), asset.getId());
        return new MediaResponse(asset.getId(), asset.getObjectKey(), asset.getPurpose(), asset.getContentType(),
                asset.getSizeBytes(), asset.getCreatedAt(), contentUrl, includeUrl ? presignDownload(asset.getObjectKey()) : null);
    }

    private String presignDownload(String objectKey) {
        GetObjectRequest getObject = GetObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build();
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(properties.downloadUrlTtl())
                        .getObjectRequest(getObject)
                        .build())
                .url()
                .toString();
    }

    private ImageFormat validateAndDetectImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > properties.maxFileSizeBytes()) {
            throw new MediaServiceException(ErrorCode.INVALID_MEDIA_FILE);
        }
        ImageFormat format;
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            format = ImageFormat.fromHeader(header);
        } catch (IOException exception) {
            throw new MediaServiceException(ErrorCode.INVALID_MEDIA_FILE);
        }
        if (format == null || (file.getContentType() != null && !format.contentType().equalsIgnoreCase(file.getContentType()))) {
            throw new MediaServiceException(ErrorCode.INVALID_MEDIA_FILE);
        }
        return format;
    }

    private void ensureStorageConfigured() {
        if (properties.bucket() == null || properties.bucket().isBlank()) {
            throw new MediaServiceException(ErrorCode.S3_NOT_CONFIGURED);
        }
    }

    private void deleteObjectQuietly(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket()).key(objectKey).build());
        } catch (RuntimeException ignored) {
            // A scheduled orphan cleanup can remove an object when the compensating delete also fails.
        }
    }

    private enum ImageFormat {
        JPEG("image/jpeg", "jpg"), PNG("image/png", "png"), WEBP("image/webp", "webp");

        private final String contentType;
        private final String extension;

        ImageFormat(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        String contentType() { return contentType; }
        String extension() { return extension; }

        static ImageFormat fromHeader(byte[] bytes) {
            if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return JPEG;
            if (bytes.length >= 8 && Arrays.equals(Arrays.copyOf(bytes, 8), new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) return PNG;
            if (bytes.length >= 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) return WEBP;
            return null;
        }
    }
}
