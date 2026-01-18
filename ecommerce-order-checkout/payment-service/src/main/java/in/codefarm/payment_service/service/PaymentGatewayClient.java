package in.codefarm.payment_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

/**
 * Mock Payment Gateway Client for testing.
 * In production, this would integrate with a real payment gateway (Stripe, PayPal, etc.).
 */
@Component
@Slf4j
public class PaymentGatewayClient {
    
    private final Random random = new Random();
    
    /**
     * Processes a payment through the payment gateway.
     * 
     * @param orderId Order ID
     * @param amount Payment amount
     * @param paymentMethod Payment method (e.g., "CREDIT_CARD", "DEBIT_CARD")
     * @return PaymentResult containing transaction ID and status
     */
    public PaymentResult processPayment(String orderId, BigDecimal amount, String paymentMethod) {
        log.info("Processing payment: orderId={}, amount={}, method={}", orderId, amount, paymentMethod);
        
        // Simulate payment gateway processing delay
        try {
            Thread.sleep(100 + random.nextInt(200)); // 100-300ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock payment processing logic
        // For demo: 90% success rate, 10% failure rate
        boolean success = random.nextDouble() > 0.1;
        
        if (success) {
            String transactionId = "txn-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
            log.info("Payment successful: orderId={}, transactionId={}", orderId, transactionId);
            return PaymentResult.success(transactionId);
        } else {
            String failureReason = "Payment declined by gateway";
            log.warn("Payment failed: orderId={}, reason={}", orderId, failureReason);
            return PaymentResult.failure(failureReason);
        }
    }
    
    /**
     * Result of payment gateway processing.
     */
    public static class PaymentResult {
        private final boolean success;
        private final String transactionId;
        private final String failureReason;
        
        private PaymentResult(boolean success, String transactionId, String failureReason) {
            this.success = success;
            this.transactionId = transactionId;
            this.failureReason = failureReason;
        }
        
        public static PaymentResult success(String transactionId) {
            return new PaymentResult(true, transactionId, null);
        }
        
        public static PaymentResult failure(String failureReason) {
            return new PaymentResult(false, null, failureReason);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getTransactionId() {
            return transactionId;
        }
        
        public String getFailureReason() {
            return failureReason;
        }
    }
}

