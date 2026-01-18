package in.codefarm.payment_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data payload for PaymentFailedEvent published to payments topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEventData {
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String failureReason;
}

