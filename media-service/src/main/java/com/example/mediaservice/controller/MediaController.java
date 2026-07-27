package com.example.mediaservice.controller;

import com.example.mediaservice.dto.MediaResponse;
import com.example.mediaservice.entity.MediaPurpose;
import com.example.mediaservice.service.MediaAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaAssetService mediaAssetService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse uploadImage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam MediaPurpose purpose,
            @RequestParam("file") MultipartFile file
    ) {
        assertPurposeAllowed(jwt, purpose);
        return mediaAssetService.uploadImage(jwt.getSubject(), purpose, file);
    }

    @GetMapping("/{mediaId}/download-url")
    public MediaResponse getDownloadUrl(
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return mediaAssetService.getDownloadUrl(mediaId, jwt == null ? null : jwt.getSubject());
    }

    @GetMapping("/{mediaId}/content")
    public ResponseEntity<Void> getContent(
            @PathVariable UUID mediaId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String downloadUrl = mediaAssetService.getContentUrl(mediaId, jwt == null ? null : jwt.getSubject());
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(downloadUrl)).build();
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID mediaId, @AuthenticationPrincipal Jwt jwt) {
        mediaAssetService.delete(mediaId, jwt.getSubject());
    }

    private void assertPurposeAllowed(Jwt jwt, MediaPurpose purpose) {
        if (purpose != MediaPurpose.PRODUCT_IMAGE) return;
        boolean isSeller = jwt.getClaimAsStringList("roles") != null && jwt.getClaimAsStringList("roles").stream()
                .anyMatch(role -> "SELLER".equals(role) || "ROLE_SELLER".equals(role));
        if (!isSeller) throw new com.example.mediaservice.exception.MediaServiceException(
                com.example.mediaservice.exception.ErrorCode.MEDIA_ACCESS_DENIED);
    }
}
