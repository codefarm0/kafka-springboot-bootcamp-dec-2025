package in.codefarm.notification.service.as.consumer.exception;

public class TransientDownstreamException extends RuntimeException {
    public TransientDownstreamException(String message) {
        super(message);
    }
}
