package com.example.promotionservice.repository;

import com.example.promotionservice.entity.GeneralFlashSaleNotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GeneralFlashSaleNotificationSubscriptionRepository extends JpaRepository<GeneralFlashSaleNotificationSubscription, UUID> {

    boolean existsByUserId(String userId);
}
