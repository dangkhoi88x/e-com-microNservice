package com.example.productservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "product_option_values", uniqueConstraints = @UniqueConstraint(columnNames = {"option_id", "value"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductOptionValue extends AbstractEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "option_id", nullable = false) private ProductOption option;
    @Column(nullable = false) private String value;
    @Column(nullable = false) private String displayValue;
    private String colorHex;
    private String imageUrl;
    @Builder.Default private Integer displayOrder = 0;
    @Builder.Default private Boolean active = true;
}
