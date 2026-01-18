package in.codefarm.inventory_service.consumer;

import org.springframework.kafka.annotation.BackOff;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import in.codefarm.inventory_service.event.PaymentProcessedEventData;
import in.codefarm.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import in.codefarm.inventory_service.exception.InventoryEventPublishingException;
import in.codefarm.inventory_service.exception.OrderItemsMissingException;
import in.codefarm.inventory_service.exception.ProductNotFoundException;
import in.codefarm.inventory_service.exception.InsufficientInventoryException;

/**
 * Event consumer for Inventory Service.
 * Listens to payments topic and processes inventory reservation when payment is processed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumer {
    
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    
    /**
     * Handles payment events from the payments topic.
     * Triggers inventory reservation when payment is processed.
     * 
     * Retry strategy:
     * - Retries 3 times for InventoryEventPublishingException (transient failures)
     * - Sends directly to DLT for business logic failures (OrderItemsMissingException, 
     *   ProductNotFoundException, InsufficientInventoryException)
     */
    @RetryableTopic(
        attempts = "3",  // Retry 3 times (total 4 attempts: 1 initial + 3 retries)
        backOff = @BackOff(delay = 1000, multiplier = 2.0),  // 1s, 2s, 4s delays
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        exclude = {OrderItemsMissingException.class, ProductNotFoundException.class, InsufficientInventoryException.class}  // Send business failures directly to DLT
    )
    @KafkaListener(
        topics = "payments",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String eventId = null;
        String orderId = null;
        PaymentProcessedEventData paymentData = null;
        
        try {
            // Parse event wrapper - use a more flexible approach
            // First parse as a generic map to handle eventTime string properly
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> eventMap = objectMapper.readValue(
                eventJson,
                java.util.Map.class
            );
            
            eventType = (String) eventMap.get("eventType");
            eventId = (String) eventMap.get("eventId");
            
            // Parse the data field as PaymentProcessedEventData
            Object dataObj = eventMap.get("data");
            if (dataObj != null) {
                paymentData = objectMapper.convertValue(dataObj, PaymentProcessedEventData.class);
                if (paymentData != null) {
                    orderId = paymentData.getOrderId();
                }
            }
            
            log.info("Received payment event: type={}, id={}, orderId={}, partition={}, offset={}",
                eventType,
                eventId,
                orderId,
                consumerRecord.partition(),
                consumerRecord.offset());
            
            // Only process payment.processed events
            if (!"com.ecommerce.payment.processed".equals(eventType)) {
                log.debug("Ignoring event type: {}, expected: com.ecommerce.payment.processed", eventType);
                acknowledgment.acknowledge();
                return;
            }
            
            if (orderId == null || paymentData == null) {
                log.warn("PaymentProcessed event missing orderId or data, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            log.info("Processing inventory reservation for order: orderId={}, paymentId={}, items={}", 
                orderId, paymentData.getPaymentId(), paymentData.getItems() != null ? paymentData.getItems().size() : 0);
            
            // Reserve inventory (with idempotency check using event ID)
            // Pass order items, shipping address, and customer ID from payment event
            inventoryService.reserveInventory(eventId, orderId, paymentData.getItems(), 
                paymentData.getShippingAddress(), paymentData.getCustomerId());
            
            log.info("Inventory reservation processing completed: orderId={}", orderId);
            acknowledgment.acknowledge();
            
        } catch (OrderItemsMissingException | ProductNotFoundException | InsufficientInventoryException e) {
            // Business logic failure: publish failure event and acknowledge
            log.error("Business logic failure processing payment event: type={}, id={}, orderId={}, error={}",
                eventType, eventId, orderId, e.getMessage());
            
            // Extract productId from exception if available
            String productId = null;
            if (e instanceof ProductNotFoundException) {
                productId = ((ProductNotFoundException) e).getProductId();
            } else if (e instanceof InsufficientInventoryException) {
                productId = ((InsufficientInventoryException) e).getProductId();
            }
            
            // Publish failure event outside transaction
            inventoryService.publishInventoryReservationFailedEvent(orderId, e.getMessage(), productId);
            
            // Acknowledge - message goes to DLT
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment event: type={}, id={}, orderId={}, eventJson={}",
                eventType, eventId, orderId, eventJson, e);
            // Log the root cause
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage(), e.getCause());
            }
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Failed to process payment event: " + e.getMessage(), e);
        }
    }
}

