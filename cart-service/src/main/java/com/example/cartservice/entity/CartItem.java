package com.example.cartservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"cart_id", "product_id", "variant_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CartItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    private String productId;

    private String variantId;

    @Column(nullable = false)
    private String productName;

    private String variantName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false)
    private Integer quantity;

    @Builder.Default
    private Boolean selected = true;

    /** The order currently processing this item; null means the item is still editable in the cart. */
    private String checkoutOrderId;
}
