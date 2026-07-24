package event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerShopStatusChangedEvent {
    private String shopId;
    private String ownerUserId;
    private String previousStatus;
    private String status;
    private Instant occurredAt;
}
