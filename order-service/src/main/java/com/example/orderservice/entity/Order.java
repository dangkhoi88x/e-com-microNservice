package com.example.orderservice.entity;

import com.example.orderservice.common.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, updatable = false, length = 32)
    private String orderCode;

    @Column(nullable = false)
    private String userId;

    @Column(name = "seller_id")
    private String sellerId;

    @Column(name = "shop_id")
    private String shopId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2, columnDefinition = "numeric(19,2) default 0")
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 2, columnDefinition = "numeric(19,2) default 0")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(length = 60)
    private String promotionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private String shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }
}
