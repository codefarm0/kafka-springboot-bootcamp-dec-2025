package in.codefarm.streams.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPlacedEvent(
    String orderId,
    String customerId,
    String productId,
    Integer quantity,
    BigDecimal totalAmount,
    LocalDateTime orderDate
) {
}

