package in.codefarm.inventory_service.exception;

/**
 * Exception thrown when publishing inventory events fails.
 * This is a transient failure (network issues, Kafka unavailable) - retryable.
 */
public class InventoryEventPublishingException extends RuntimeException {
    
    public InventoryEventPublishingException(String message) {
        super(message);
    }
    
    public InventoryEventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}

