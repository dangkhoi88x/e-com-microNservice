package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.math.BigDecimal;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    @Query(value = "select count(*) as totalOrders, count(*) filter (where status = 'COMPLETED') as completedOrders, coalesce(sum(total_amount) filter (where status = 'COMPLETED'), 0) as revenue from orders where created_at >= :from and created_at < :to", nativeQuery = true)
    AnalyticsSummary getAnalyticsSummary(Instant from, Instant to);

    @Query(value = "select count(*) as totalOrders, count(*) filter (where status = 'COMPLETED') as completedOrders, coalesce(sum(total_amount) filter (where status = 'COMPLETED'), 0) as revenue from orders where seller_id = :sellerId and created_at >= :from and created_at < :to", nativeQuery = true)
    AnalyticsSummary getSellerAnalyticsSummary(String sellerId, Instant from, Instant to);

    @Query(value = "select status as status, count(*) as total from orders where created_at >= :from and created_at < :to group by status", nativeQuery = true)
    List<StatusCount> countByStatus(Instant from, Instant to);

    @Query(value = "select status as status, count(*) as total from orders where seller_id = :sellerId and created_at >= :from and created_at < :to group by status", nativeQuery = true)
    List<StatusCount> countSellerByStatus(String sellerId, Instant from, Instant to);

    @Query(value = "select to_char((created_at at time zone 'Asia/Ho_Chi_Minh')::date, 'YYYY-MM-DD') as date, coalesce(sum(total_amount), 0) as revenue, count(*) as total from orders where status = 'COMPLETED' and created_at >= :from and created_at < :to group by 1 order by 1", nativeQuery = true)
    List<DailyRevenue> getDailyRevenue(Instant from, Instant to);

    @Query(value = "select oi.product_id as productId, max(oi.product_name) as name, sum(oi.quantity) as quantitySold, coalesce(sum(oi.subtotal), 0) as revenue from order_items oi join orders o on o.id = oi.order_id where o.status = 'COMPLETED' and o.created_at >= :from and o.created_at < :to group by oi.product_id order by revenue desc limit 5", nativeQuery = true)
    List<TopProduct> getTopProducts(Instant from, Instant to);

    @Query(value = "select oi.product_id as productId, max(oi.product_name) as name, sum(oi.quantity) as quantitySold, coalesce(sum(oi.subtotal), 0) as revenue from order_items oi join orders o on o.id = oi.order_id where o.seller_id = :sellerId and o.status = 'COMPLETED' and o.created_at >= :from and o.created_at < :to group by oi.product_id order by revenue desc limit 5", nativeQuery = true)
    List<TopProduct> getSellerTopProducts(String sellerId, Instant from, Instant to);

    interface AnalyticsSummary {
        long getTotalOrders();
        long getCompletedOrders();
        BigDecimal getRevenue();
    }

    interface StatusCount {
        String getStatus();
        long getTotal();
    }

    interface DailyRevenue {
        String getDate();
        BigDecimal getRevenue();
        long getTotal();
    }

    interface TopProduct {
        String getProductId();
        String getName();
        long getQuantitySold();
        BigDecimal getRevenue();
    }
    Page<Order> findByUserId(String userId, Pageable pageable);
    Page<Order> findBySellerId(String sellerId, Pageable pageable);
    List<Order> findBySellerIdAndCreatedAtBetween(String sellerId, Instant from, Instant to);
    List<Order> findByCreatedAtBetween(Instant from, Instant to);

    Page<Order> findByPromotionCodeIgnoreCase(String promotionCode, Pageable pageable);

    Optional<Order> findByIdAndUserId(String id, String userId);

    boolean existsByOrderCode(String orderCode);

    Optional<Order> findByOrderCodeAndUserId(String orderCode, String userId);
    Optional<Order> findByOrderCodeIgnoreCase(String orderCode);


}
