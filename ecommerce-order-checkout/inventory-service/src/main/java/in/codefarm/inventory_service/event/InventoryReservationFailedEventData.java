package in.codefarm.inventory_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data payload for InventoryReservationFailedEvent published to inventory topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationFailedEventData {
    private String orderId;
    private String failureReason;
    private String productId;  // Product that caused the failure
}

