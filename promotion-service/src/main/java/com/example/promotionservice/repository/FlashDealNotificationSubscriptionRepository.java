package com.example.promotionservice.repository;

import com.example.promotionservice.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;

public interface FlashDealNotificationSubscriptionRepository extends JpaRepository<FlashDealNotificationSubscription, UUID> {
    boolean existsByUserIdAndFlashDealId(String userId, UUID flashDealId);
    List<FlashDealNotificationSubscription> findAllByUserId(String userId);
    List<FlashDealNotificationSubscription> findAllByNotifiedAtIsNullAndFlashDealStatusAndFlashDealStartAtAfterAndFlashDealStartAtLessThanEqual(FlashDealStatus status, Instant after, Instant before);
}
