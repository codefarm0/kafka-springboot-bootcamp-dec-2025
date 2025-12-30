package in.codefarm.order.service.as.producer.dto;

public record PaymentResponse(
    String status,
    String message,
    String orderId,
    String transactionId
) {
    public static PaymentResponse success(String orderId, String transactionId) {
        return new PaymentResponse("success", "Payment processed successfully", orderId, transactionId);
    }
    
    public static PaymentResponse error(String message, String orderId) {
        return new PaymentResponse("error", message, orderId, null);
    }
}

