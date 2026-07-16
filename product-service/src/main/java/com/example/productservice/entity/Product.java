package com.example.productservice.entity;

import com.example.productservice.common.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnDefinition;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name ="products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
    public class Product extends  AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private BigDecimal price;
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<ProductImage> images;

    @Column(nullable = false, unique = true)
    private String slug;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

}
