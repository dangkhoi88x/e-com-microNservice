package com.example.productservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "product_options", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductOption extends AbstractEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String displayName;
    @Column(nullable = false) private String displayType;
    @Builder.Default private Integer displayOrder = 0;
    @Builder.Default private Boolean required = true;
    @OneToMany(mappedBy = "option", cascade = CascadeType.ALL, orphanRemoval = true) @Builder.Default
    private List<ProductOptionValue> values = new ArrayList<>();
}
