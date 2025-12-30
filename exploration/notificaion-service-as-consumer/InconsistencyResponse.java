package in.codefarm.notification.service.as.consumer.dto;

import java.util.List;

public record InconsistencyResponse(
    int totalInconsistencies,
    List<InconsistencyDetail> inconsistencies,
    String message
) {
    public record InconsistencyDetail(
        String type,
        String paymentId,
        String orderId,
        String customerId,
        String issue
    ) {
    }
}

