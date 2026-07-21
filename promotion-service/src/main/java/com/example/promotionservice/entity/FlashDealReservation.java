package com.example.promotionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity
@Table(name = "flash_deal_reservations", uniqueConstraints = @UniqueConstraint(name = "uk_flash_reservation_order_item", columnNames = {"order_id", "flash_deal_item_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FlashDealReservation extends AbstractEntity {
    @Column(name = "order_id", nullable = false, length = 64) private String orderId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flash_deal_item_id", nullable = false) private FlashDealItem flashDealItem;
    @Column(nullable = false, length = 64) private String productId;
    @Column(length = 64) private String variantId;
    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal salePrice;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private FlashDealReservationStatus status;
}
