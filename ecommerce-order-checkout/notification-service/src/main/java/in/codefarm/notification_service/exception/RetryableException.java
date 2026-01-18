package in.codefarm.notification_service.exception;

/**
 * Exception for retryable errors (transient failures that might succeed on retry).
 * Examples: Database connection timeout, network issues, temporary service unavailability.
 */
public class RetryableException extends RuntimeException {
    
    public RetryableException(String message) {
        super(message);
    }
    
    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}

