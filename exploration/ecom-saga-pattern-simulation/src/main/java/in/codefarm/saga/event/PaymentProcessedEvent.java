package in.codefarm.saga.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentProcessedEvent(
    String paymentId,
    String orderId,
    String customerId,
    BigDecimal amount,
    String status,  // "SUCCESS" or "FAILED"
    LocalDateTime processedAt
) {
}

