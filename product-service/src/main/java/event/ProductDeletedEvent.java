package event;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ProductDeletedEvent {
    private String productId;

}
