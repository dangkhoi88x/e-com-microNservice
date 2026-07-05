package event;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ProductCreatedEvent {
    private String productId;
    private String name;
    private String description;
    private Double price;
    private String categoryName;
    private String thumbnailUrl;
    private String categoryId;
    private String status;
    private Boolean inStock;
    private Instant createdAt;

}
