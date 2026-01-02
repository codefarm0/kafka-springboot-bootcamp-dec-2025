package in.codefarm.saga.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRefundedEvent(
    String paymentId,
    String orderId,
    BigDecimal amount,
    LocalDateTime refundedAt
) {
}

