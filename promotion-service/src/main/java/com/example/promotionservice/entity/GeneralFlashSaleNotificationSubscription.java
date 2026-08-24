package com.example.promotionservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "general_flash_sale_notification_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_general_flash_sale_notification_user",
                columnNames = "user_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GeneralFlashSaleNotificationSubscription extends AbstractEntity {

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;
}
