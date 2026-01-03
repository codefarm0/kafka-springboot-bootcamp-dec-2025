package in.codefarm.streams.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnrichedOrderEvent(
    String orderId,
    String customerId,
    String productId,
    Integer quantity,
    BigDecimal totalAmount,
    LocalDateTime orderDate,
    String orderCategory,  // "HIGH_VALUE" or "REGULAR"
    LocalDateTime processedAt,
    String processingStatus  // "PROCESSED", "PENDING", etc.
) {
}

