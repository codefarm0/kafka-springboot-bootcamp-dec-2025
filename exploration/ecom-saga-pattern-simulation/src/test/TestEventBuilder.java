package in.codefarm.saga.testutil;

import in.codefarm.saga.event.*;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test utility class for building test events.
 * 
 * This builder helps create consistent test data for saga pattern tests.
 * All events are created with proper metadata and realistic test values.
 */
public class TestEventBuilder {
    
    private static final JsonMapper jsonMapper = JsonMapper.builder().build();
    
    /**
     * Creates a test OrderPlacedEvent with default values.
     * 
     * @return OrderPlacedEvent with test data
     */
    public static OrderPlacedEvent defaultOrderPlacedEvent() {
        return new OrderPlacedEvent(
            "order-" + UUID.randomUUID().toString().substring(0, 8),
            "customer-123",
            "product-456",
            2,
            BigDecimal.valueOf(99.99),
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a test OrderPlacedEvent with custom amount.
     * Useful for testing payment failure scenarios (amount > 1000).
     * 
     * @param amount Order amount
     * @return OrderPlacedEvent with specified amount
     */
    public static OrderPlacedEvent orderPlacedEventWithAmount(BigDecimal amount) {
        return new OrderPlacedEvent(
            "order-" + UUID.randomUUID().toString().substring(0, 8),
            "customer-123",
            "product-456",
            2,
            amount,
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a test PaymentProcessedEvent with SUCCESS status.
     * 
     * @param orderId Order ID
     * @param customerId Customer ID
     * @param amount Payment amount
     * @return PaymentProcessedEvent with SUCCESS status
     */
    public static PaymentProcessedEvent paymentProcessedSuccess(String orderId, String customerId, BigDecimal amount) {
        return new PaymentProcessedEvent(
            "payment-" + UUID.randomUUID().toString().substring(0, 8),
            orderId,
            customerId,
            amount,
            "SUCCESS",
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a test PaymentProcessedEvent with FAILED status.
     * Used for testing compensation scenarios.
     * 
     * @param orderId Order ID
     * @param customerId Customer ID
     * @param amount Payment amount
     * @return PaymentProcessedEvent with FAILED status
     */
    public static PaymentProcessedEvent paymentProcessedFailed(String orderId, String customerId, BigDecimal amount) {
        return new PaymentProcessedEvent(
            "payment-" + UUID.randomUUID().toString().substring(0, 8),
            orderId,
            customerId,
            amount,
            "FAILED",
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a test PaymentRefundedEvent.
     * Used for testing compensation when inventory reservation fails.
     * 
     * @param orderId Order ID
     * @param customerId Customer ID
     * @param amount Refund amount
     * @return PaymentRefundedEvent
     */
    public static PaymentRefundedEvent paymentRefunded(String orderId, String customerId, BigDecimal amount) {
        return new PaymentRefundedEvent(
            "payment-" + UUID.randomUUID().toString().substring(0, 8),
            customerId,
            amount,
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a test InventoryReservedEvent with RESERVED status.
     * 
     * @param orderId Order ID
     * @param productId Product ID
     * @param quantity Reserved quantity
     * @return InventoryReservedEvent with RESERVED status
     */
    public static InventoryReservedEvent inventoryReserved(String orderId, String productId, Integer quantity) {
        return new InventoryReservedEvent(
            "reservation-" + UUID.randomUUID().toString().substring(0, 8),
            orderId,
            productId,
            quantity,
            "RESERVED",
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a test InventoryReservedEvent with UNAVAILABLE status.
     * Used for testing inventory failure scenarios.
     * 
     * @param orderId Order ID
     * @param productId Product ID
     * @param quantity Requested quantity
     * @return InventoryReservedEvent with UNAVAILABLE status
     */
    public static InventoryReservedEvent inventoryUnavailable(String orderId, String productId, Integer quantity) {
        return new InventoryReservedEvent(
            "reservation-" + UUID.randomUUID().toString().substring(0, 8),
            orderId,
            productId,
            quantity,
            "UNAVAILABLE",
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a test OrderCancelledEvent.
     * Used for testing compensation scenarios.
     * 
     * @param orderId Order ID
     * @param reason Cancellation reason
     * @return OrderCancelledEvent
     */
    public static OrderCancelledEvent orderCancelled(String orderId, String reason) {
        return new OrderCancelledEvent(
            orderId,
            reason,
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates EventWrapper for OrderPlacedEvent.
     * Wraps the event with proper metadata for saga tracking.
     * 
     * @param payload OrderPlacedEvent payload
     * @param transactionId Saga transaction ID
     * @return EventWrapper containing OrderPlacedEvent
     */
    public static EventWrapper<OrderPlacedEvent> wrapOrderPlaced(OrderPlacedEvent payload, String transactionId) {
        EventMetadata metadata = new EventMetadata(
            "OrderPlaced",
            "1.0",
            "order-service",
            transactionId,
            LocalDateTime.now()
        );
        return new EventWrapper<>(metadata, payload);
    }
    
    /**
     * Creates EventWrapper for PaymentProcessedEvent.
     * 
     * @param payload PaymentProcessedEvent payload
     * @param transactionId Saga transaction ID
     * @return EventWrapper containing PaymentProcessedEvent
     */
    public static EventWrapper<PaymentProcessedEvent> wrapPaymentProcessed(
        PaymentProcessedEvent payload, 
        String transactionId
    ) {
        EventMetadata metadata = new EventMetadata(
            "PaymentProcessed",
            "1.0",
            "payment-service",
            transactionId,
            LocalDateTime.now()
        );
        return new EventWrapper<>(metadata, payload);
    }
    
    /**
     * Creates EventWrapper for PaymentRefundedEvent.
     * 
     * @param payload PaymentRefundedEvent payload
     * @param transactionId Saga transaction ID
     * @return EventWrapper containing PaymentRefundedEvent
     */
    public static EventWrapper<PaymentRefundedEvent> wrapPaymentRefunded(
        PaymentRefundedEvent payload,
        String transactionId
    ) {
        EventMetadata metadata = new EventMetadata(
            "PaymentRefunded",
            "1.0",
            "payment-service",
            transactionId,
            LocalDateTime.now()
        );
        return new EventWrapper<>(metadata, payload);
    }
    
    /**
     * Creates EventWrapper for InventoryReservedEvent.
     * 
     * @param payload InventoryReservedEvent payload
     * @param transactionId Saga transaction ID
     * @return EventWrapper containing InventoryReservedEvent
     */
    public static EventWrapper<InventoryReservedEvent> wrapInventoryReserved(
        InventoryReservedEvent payload,
        String transactionId
    ) {
        EventMetadata metadata = new EventMetadata(
            "InventoryReserved",
            "1.0",
            "inventory-service",
            transactionId,
            LocalDateTime.now()
        );
        return new EventWrapper<>(metadata, payload);
    }
    
    /**
     * Generates a unique transaction ID for saga tracking.
     * 
     * @return Transaction ID string
     */
    public static String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
}

