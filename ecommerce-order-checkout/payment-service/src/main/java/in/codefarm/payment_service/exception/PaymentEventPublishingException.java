package in.codefarm.payment_service.exception;

/**
 * Exception thrown when publishing payment events fails.
 * This is a transient failure (network issues, Kafka unavailable) - retryable.
 */
public class PaymentEventPublishingException extends RuntimeException {
    
    public PaymentEventPublishingException(String message) {
        super(message);
    }
    
    public PaymentEventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}

