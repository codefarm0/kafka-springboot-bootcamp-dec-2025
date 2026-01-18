package in.codefarm.shipping_service.exception;

/**
 * Exception thrown when publishing shipping events fails.
 * This is a transient failure (network issues, Kafka unavailable) - retryable.
 */
public class ShippingEventPublishingException extends RuntimeException {
    
    public ShippingEventPublishingException(String message) {
        super(message);
    }
    
    public ShippingEventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}

