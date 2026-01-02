package in.codefarm.saga.event;

import java.time.LocalDateTime;

public record EventMetadata(
    String eventType,        // "OrderPlaced", "PaymentProcessed", etc.
    String eventVersion,     // "1.0"
    String source,           // "order-service", "payment-service", etc.
    String transactionId,    // For saga correlation
    LocalDateTime timestamp  // Event creation time
) {
}

