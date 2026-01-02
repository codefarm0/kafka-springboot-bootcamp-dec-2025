package in.codefarm.saga.event;

import java.time.LocalDateTime;

public record InventoryReservedEvent(
    String reservationId,
    String orderId,
    String productId,
    Integer quantity,
    String status,  // "RESERVED" or "UNAVAILABLE"
    LocalDateTime reservedAt
) {
}

