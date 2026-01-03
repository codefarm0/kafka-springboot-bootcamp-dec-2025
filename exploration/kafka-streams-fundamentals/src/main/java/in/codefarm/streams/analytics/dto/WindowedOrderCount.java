package in.codefarm.streams.analytics.dto;

import java.time.Instant;

public record WindowedOrderCount(
    String customerId,
    Instant windowStart,
    Instant windowEnd,
    Long count
) {
}

