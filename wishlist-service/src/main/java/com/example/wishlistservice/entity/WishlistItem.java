package com.example.wishlistservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity @Table(name = "wishlist_items") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class WishlistItem extends AbstractEntity {
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "product_id", nullable = false) private String productId;
    @Column(name = "variant_id") private String variantId;
    @Column(nullable = false) private String productName;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal price;
    private String imageUrl;
    private String categoryName;
}
