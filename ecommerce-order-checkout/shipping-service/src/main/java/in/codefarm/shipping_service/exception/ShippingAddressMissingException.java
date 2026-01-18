package in.codefarm.shipping_service.exception;

/**
 * Exception thrown when shipping address is missing or empty.
 * This is a business logic failure - non-retryable, send directly to DLT.
 */
public class ShippingAddressMissingException extends RuntimeException {
    
    public ShippingAddressMissingException(String message) {
        super(message);
    }
    
    public ShippingAddressMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}

