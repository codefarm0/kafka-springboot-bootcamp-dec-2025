package in.codefarm.shipping_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data payload for ShippingLabelCreatedEvent published to shipping topic.
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

