package in.codefarm.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Mock SMS Service for testing.
 * In production, this would integrate with a real SMS service (Twilio, AWS SNS, etc.).
 */
@Component
@Slf4j
public class SmsService {
    
    private final Random random = new Random();
    
    /**
     * Sends an SMS notification.
     * 
     * @param to Recipient phone number
     * @param message SMS message
     * @return true if SMS sent successfully, false otherwise
     */
    public boolean sendSms(String to, String message) {
        log.info("Sending SMS: to={}, message={}", to, message.substring(0, Math.min(50, message.length())));
        
        // Simulate SMS sending delay
        try {
            Thread.sleep(30 + random.nextInt(70)); // 30-100ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock SMS sending logic
        // For demo: 90% success rate, 10% failure rate
        boolean success = random.nextDouble() > 0.1;
        
        if (success) {
            log.info("SMS sent successfully: to={}", to);
            return true;
        } else {
            log.warn("SMS sending failed: to={}", to);
            return false;
        }
    }
}

