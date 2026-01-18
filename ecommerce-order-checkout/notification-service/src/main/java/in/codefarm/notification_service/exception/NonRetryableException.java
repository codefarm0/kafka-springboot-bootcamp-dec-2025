package in.codefarm.notification_service.exception;

/**
 * Exception for non-retryable errors (business logic failures, validation errors).
 * These should go directly to DLT without retry.
 * Examples: Missing required data, invalid business rules, data validation failures.
 */
public class NonRetryableException extends RuntimeException {
    
    public NonRetryableException(String message) {
        super(message);
    }
    
    public NonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}

