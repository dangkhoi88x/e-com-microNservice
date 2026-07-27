package com.example.mediaservice.entity;

public enum MediaPurpose {
    PRODUCT_IMAGE,
    REVIEW_IMAGE,
    AVATAR,
    SELLER_DOCUMENT;

    public boolean isPublic() {
        return this == PRODUCT_IMAGE || this == REVIEW_IMAGE || this == AVATAR;
    }
}
