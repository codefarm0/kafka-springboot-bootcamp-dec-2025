package in.codefarm.notification_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data payload for ShippingLabelCreatedEvent consumed from shipping topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingLabelCreatedEventData {
    private String shipmentId;
    private String orderId;
    private String customerId;
    private String trackingNumber;
    private String carrier;
    private String shippingAddress;
}

