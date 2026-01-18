package in.codefarm.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data payload for PaymentProcessedEvent published to payments topic.
 * Includes order items and shipping address for downstream services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEventData {
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String transactionId;
    private String paymentMethod;
    // Include order items for inventory service
    private List<OrderItemData> items;
    // Include shipping address for shipping service
    private String shippingAddress;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemData {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}

