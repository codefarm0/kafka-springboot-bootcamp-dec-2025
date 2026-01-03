package in.codefarm.notification.service.as.consumer.dto;

public record ConsistencyCheckResponse(
    String orderId,
    boolean isConsistent,
    boolean hasOrder,
    boolean hasPayment,
    String message
) {
}

