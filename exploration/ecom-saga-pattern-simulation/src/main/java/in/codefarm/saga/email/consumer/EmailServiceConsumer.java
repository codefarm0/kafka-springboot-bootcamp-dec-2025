package in.codefarm.saga.email.consumer;

import tools.jackson.databind.json.JsonMapper;
import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.InventoryReservedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EmailServiceConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(EmailServiceConsumer.class);
    
    private final JsonMapper jsonMapper;
    
    public EmailServiceConsumer(@Qualifier("consumerJsonMapper") JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }
    
    @KafkaListener(
        topics = "inventory",
        groupId = "email-service-group",
        containerFactory = "eventWrapperKafkaListenerContainerFactory"
    )
    public void handleInventoryReserved(@Payload EventWrapper<?> wrapper) {
        try {
            InventoryReservedEvent event = jsonMapper.convertValue(wrapper.payload(), InventoryReservedEvent.class);
            String transactionId = wrapper.metadata().transactionId();
            
            log.info("Email Service: Received InventoryReserved event - OrderId: {}, Status: {}, TransactionId: {}", 
                event.orderId(), event.status(), transactionId);
            
            // Only send email if inventory was successfully reserved
            if (!"RESERVED".equals(event.status())) {
                log.warn("Email Service: Inventory not reserved for order: {} - skipping email", 
                    event.orderId());
                return;
            }
            
            sendConfirmationEmail(event, transactionId);
            log.info("Email Service: Confirmation email sent - OrderId: {}, TransactionId: {}", 
                event.orderId(), transactionId);
        } catch (Exception e) {
            log.error("Email Service: Error sending email", e);
        }
    }
    
    private void sendConfirmationEmail(InventoryReservedEvent event, String transactionId) {
        // Simulate email sending
        // In real system: call email service, send template, etc.
        log.info("Sending confirmation email for order: {}", event.orderId());
        log.info("Email content: Your order {} has been confirmed and inventory reserved! TransactionId: {}", 
            event.orderId(), transactionId);
    }
}

