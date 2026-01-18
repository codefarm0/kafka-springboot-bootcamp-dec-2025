package in.codefarm.shipping_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data payload for InventoryReservedEvent consumed from inventory topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedEventData {
    private String orderId;
    private List<ReservedItemData> reservedItems;
    // Shipping address for shipping service
    private String shippingAddress;
    // Customer ID for shipping service
    private String customerId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservedItemData {
        private String productId;
        private String productName;
        private Integer quantity;
        private String reservationId;
    }
}

