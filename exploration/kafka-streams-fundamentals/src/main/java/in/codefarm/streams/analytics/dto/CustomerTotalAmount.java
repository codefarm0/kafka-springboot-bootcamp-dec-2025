package in.codefarm.streams.analytics.dto;

import java.math.BigDecimal;

public record CustomerTotalAmount(
    String customerId,
    BigDecimal totalAmount
) {
}

