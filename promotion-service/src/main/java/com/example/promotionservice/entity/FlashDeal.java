package com.example.promotionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flash_deals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FlashDeal extends AbstractEntity {
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 500) private String description;
    @Enumerated(EnumType.STRING) @Column(name = "sale_type", length = 20) private SaleType saleType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private FlashDealStatus status;
    @Column(nullable = false) private Instant startAt;
    @Column(nullable = false) private Instant endAt;
    @OneToMany(mappedBy = "flashDeal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<FlashDealItem> items = new ArrayList<>();
    public void replaceItems(List<FlashDealItem> next) { items.clear(); next.forEach(item -> { item.setFlashDeal(this); items.add(item); }); }
    public SaleType effectiveSaleType() { return saleType == null ? SaleType.FLASH : saleType; }
}
