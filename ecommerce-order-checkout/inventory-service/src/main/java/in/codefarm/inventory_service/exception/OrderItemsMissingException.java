package in.codefarm.inventory_service.exception;

/**
 * Exception thrown when order items are missing or empty.
 * This is a business logic failure - non-retryable, send directly to DLT.
 */
public class OrderItemsMissingException extends RuntimeException {
    
    public OrderItemsMissingException(String message) {
        super(message);
    }
    
    public OrderItemsMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}

