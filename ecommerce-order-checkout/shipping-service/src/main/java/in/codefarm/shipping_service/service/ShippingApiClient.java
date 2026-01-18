package in.codefarm.shipping_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Mock Shipping API Client for testing.
 * In production, this would integrate with a real shipping carrier API (FedEx, UPS, DHL, etc.).
 */
@Component
@Slf4j
public class ShippingApiClient {
    
    private final Random random = new Random();
    
    /**
     * Creates a shipping label through the shipping carrier API.
     * 
     * @param orderId Order ID
     * @param shippingAddress Shipping address
     * @return ShippingResult containing tracking number and carrier
     */
    public ShippingResult createShippingLabel(String orderId, String shippingAddress) {
        log.info("Creating shipping label: orderId={}, address={}", orderId, shippingAddress);
        
        // Simulate shipping API processing delay
        try {
            Thread.sleep(150 + random.nextInt(250)); // 150-400ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock shipping label creation logic
        // For demo: 85% success rate, 15% failure rate
        boolean success = random.nextDouble() > 0.15;
        
        if (success) {
            String trackingNumber = "TRK" + System.currentTimeMillis() + random.nextInt(10000);
            String carrier = getRandomCarrier();
            log.info("Shipping label created: orderId={}, trackingNumber={}, carrier={}", 
                orderId, trackingNumber, carrier);
            return ShippingResult.success(trackingNumber, carrier);
        } else {
            String failureReason = "Shipping API temporarily unavailable";
            log.warn("Shipping label creation failed: orderId={}, reason={}", orderId, failureReason);
            return ShippingResult.failure(failureReason);
        }
    }
    
    private String getRandomCarrier() {
        String[] carriers = {"FedEx", "UPS", "DHL", "USPS"};
        return carriers[random.nextInt(carriers.length)];
    }
    
    /**
     * Result of shipping label creation.
     */
    public static class ShippingResult {
        private final boolean success;
        private final String trackingNumber;
        private final String carrier;
        private final String failureReason;
        
        private ShippingResult(boolean success, String trackingNumber, String carrier, String failureReason) {
            this.success = success;
            this.trackingNumber = trackingNumber;
            this.carrier = carrier;
            this.failureReason = failureReason;
        }
        
        public static ShippingResult success(String trackingNumber, String carrier) {
            return new ShippingResult(true, trackingNumber, carrier, null);
        }
        
        public static ShippingResult failure(String failureReason) {
            return new ShippingResult(false, null, null, failureReason);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getTrackingNumber() {
            return trackingNumber;
        }
        
        public String getCarrier() {
            return carrier;
        }
        
        public String getFailureReason() {
            return failureReason;
        }
    }
}

