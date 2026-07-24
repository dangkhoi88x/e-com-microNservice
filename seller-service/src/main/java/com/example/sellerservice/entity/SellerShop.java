package com.example.sellerservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(
        name = "seller_shops",
        indexes = {
                @Index(name = "idx_seller_shops_status", columnList = "status"),
                @Index(name = "idx_seller_shops_owner", columnList = "owner_user_id")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SellerShop extends AbstractEntity {
    @Column(name = "owner_user_id", nullable = false, unique = true, updatable = false)
    private String ownerUserId;

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @Column(name = "shop_name", nullable = false, length = 160)
    private String shopName;

    @Column(length = 2_000)
    private String description;

    @Column(length = 30)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 120)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerStatus status;

    @Column(name = "review_note", length = 1_000)
    private String reviewNote;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
