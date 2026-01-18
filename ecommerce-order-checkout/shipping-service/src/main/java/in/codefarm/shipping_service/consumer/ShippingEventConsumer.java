package in.codefarm.shipping_service.consumer;

import org.springframework.kafka.annotation.BackOff;
import tools.jackson.databind.ObjectMapper;
import in.codefarm.shipping_service.event.InventoryReservedEventData;
import in.codefarm.shipping_service.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import in.codefarm.shipping_service.exception.ShippingEventPublishingException;
import in.codefarm.shipping_service.exception.ShippingAddressMissingException;

/**
 * Event consumer for Shipping Service.
 * Listens to inventory topic and processes shipping label creation when inventory is reserved.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShippingEventConsumer {
    
    private final ShippingService shippingService;
    private final ObjectMapper objectMapper;
    
    /**
     * Handles inventory events from the inventory topic.
     * Triggers shipping label creation when inventory is reserved.
     * 
     * Retry strategy:
     * - Retries 3 times for ShippingEventPublishingException (transient failures)
     * - Sends directly to DLT for ShippingAddressMissingException (business logic failures)
     */
    @RetryableTopic(
        attempts = "3",  // Retry 3 times (total 4 attempts: 1 initial + 3 retries)
        backOff = @BackOff(delay = 1000, multiplier = 2.0),  // 1s, 2s, 4s delays
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        exclude = {ShippingAddressMissingException.class}  // Send business failures directly to DLT
    )
    @KafkaListener(
        topics = "inventory",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String eventId = null;
        String orderId = null;
        InventoryReservedEventData inventoryData = null;
        
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
            
            // Parse the data field as InventoryReservedEventData
            Object dataObj = eventMap.get("data");
            if (dataObj != null) {
                inventoryData = objectMapper.convertValue(dataObj, InventoryReservedEventData.class);
                if (inventoryData != null) {
                    orderId = inventoryData.getOrderId();
                }
            }
            
            log.info("Received inventory event: type={}, id={}, orderId={}, partition={}, offset={}",
                eventType,
                eventId,
                orderId,
                consumerRecord.partition(),
                consumerRecord.offset());
            
            // Only process inventory.reserved events
            if (!"com.ecommerce.inventory.reserved".equals(eventType)) {
                log.debug("Ignoring event type: {}, expected: com.ecommerce.inventory.reserved", eventType);
                acknowledgment.acknowledge();
                return;
            }
            
            if (orderId == null || inventoryData == null) {
                log.warn("InventoryReserved event missing orderId or data, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            log.info("Processing shipping label creation for order: orderId={}, reservedItems={}, shippingAddress={}", 
                orderId, inventoryData.getReservedItems() != null ? inventoryData.getReservedItems().size() : 0, 
                inventoryData.getShippingAddress());
            
            // Create shipping label (with idempotency check using event ID)
            // Pass shipping address and customer ID from inventory event
            shippingService.createShippingLabel(eventId, orderId, inventoryData.getShippingAddress(), 
                inventoryData.getCustomerId());
            
            log.info("Shipping label creation processing completed: orderId={}", orderId);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing inventory event: type={}, id={}, orderId={}, eventJson={}",
                eventType, eventId, orderId, eventJson, e);
            // Log the root cause
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage(), e.getCause());
            }
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Failed to process inventory event: " + e.getMessage(), e);
        }
    }
}

