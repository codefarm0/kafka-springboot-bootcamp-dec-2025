package in.codefarm.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Mock Email Service for testing.
 * In production, this would integrate with a real email service (SendGrid, AWS SES, etc.).
 */
@Component
@Slf4j
public class EmailService {
    
    private final Random random = new Random();
    
    /**
     * Sends an email notification.
     * 
     * @param to Recipient email address
     * @param subject Email subject
     * @param body Email body
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendEmail(String to, String subject, String body) {
        log.info("Sending email: to={}, subject={}", to, subject);
        
        // Simulate email sending delay
        try {
            Thread.sleep(50 + random.nextInt(100)); // 50-150ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Mock email sending logic
        // For demo: 95% success rate, 5% failure rate
        boolean success = random.nextDouble() > 0.05;
        
        if (success) {
            log.info("Email sent successfully: to={}, subject={}", to, subject);
            return true;
        } else {
            log.warn("Email sending failed: to={}, subject={}", to, subject);
            return false;
        }
    }
}

