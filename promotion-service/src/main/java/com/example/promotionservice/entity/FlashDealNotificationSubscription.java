package com.example.promotionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.Instant;

@Entity
@Table(name = "flash_deal_notification_subscriptions", uniqueConstraints = @UniqueConstraint(name = "uk_flash_deal_notification_user", columnNames = {"user_id", "flash_deal_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FlashDealNotificationSubscription extends AbstractEntity {
    @Column(name = "user_id", nullable = false, length = 64) private String userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flash_deal_id", nullable = false) private FlashDeal flashDeal;
    private Instant notifiedAt;
}
