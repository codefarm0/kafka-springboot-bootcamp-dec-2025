package in.codefarm.shipping_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data payload for ShippingFailedEvent published to shipping topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFailedEventData {
    private String shipmentId;
    private String orderId;
    private String failureReason;
}

