package in.codefarm.streams.analytics.dto;

public record CustomerOrderCount(
    String customerId,
    Long count
) {
}

