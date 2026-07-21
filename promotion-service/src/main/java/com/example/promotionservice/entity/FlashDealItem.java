package com.example.promotionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Entity
@Table(name = "flash_deal_items", indexes = @Index(name = "idx_flash_deal_item_deal", columnList = "flash_deal_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FlashDealItem extends AbstractEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flash_deal_id", nullable = false)
    private FlashDeal flashDeal;
    @Column(nullable = false, length = 64) private String productId;
    @Column(length = 64) private String variantId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal originalPrice;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal salePrice;
    @Column(precision = 5, scale = 2) private BigDecimal discountPercent;
    @Column(nullable = false) private Integer quota;
    @Column(name = "quota_limited") private Boolean quotaLimited;
    public boolean isQuotaLimited() { return quotaLimited == null || quotaLimited; }
}
