package event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ProductUpdatedEvent {
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
