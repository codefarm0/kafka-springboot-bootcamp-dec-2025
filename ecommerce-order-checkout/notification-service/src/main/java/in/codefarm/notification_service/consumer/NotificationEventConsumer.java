package in.codefarm.notification_service.consumer;

import tools.jackson.databind.ObjectMapper;
import in.codefarm.notification_service.event.ShippingLabelCreatedEventData;
import in.codefarm.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Event consumer for Notification Service.
 * Listens to all topics (orders, payments, inventory, shipping) to send notifications at each stage.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {
    
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    /**
     * Handles order events from the orders topic.
     */
    @KafkaListener(
        topics = "orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String orderId = null;
        
        try {
            // Parse event wrapper - use a more flexible approach
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> eventMap = objectMapper.readValue(
                eventJson,
                java.util.Map.class
            );
            
            eventType = (String) eventMap.get("eventType");
            
            // Extract orderId from data
            Object dataObj = eventMap.get("data");
            if (dataObj != null && dataObj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) dataObj;
                orderId = (String) dataMap.get("orderId");
            }
            
            log.info("Received order event: type={}, id={}, orderId={}", 
                eventType, eventMap.get("eventId"), orderId);
            
            if ("com.ecommerce.order.placed".equals(eventType)) {
                notificationService.sendOrderPlacedNotification(orderId);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order event: type={}, orderId={}",
                eventType, orderId, e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }
    
    /**
     * Handles payment events from the payments topic.
     */
    @KafkaListener(
        topics = "payments",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String orderId = null;
        
        try {
            // Parse event wrapper - use a more flexible approach
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> eventMap = objectMapper.readValue(
                eventJson,
                java.util.Map.class
            );
            
            eventType = (String) eventMap.get("eventType");
            
            // Extract orderId from data
            Object dataObj = eventMap.get("data");
            if (dataObj != null && dataObj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) dataObj;
                orderId = (String) dataMap.get("orderId");
            }
            
            log.info("Received payment event: type={}, id={}, orderId={}", 
                eventType, eventMap.get("eventId"), orderId);
            
            if ("com.ecommerce.payment.processed".equals(eventType)) {
                notificationService.sendPaymentProcessedNotification(orderId);
            } else if ("com.ecommerce.payment.failed".equals(eventType)) {
                notificationService.sendPaymentFailedNotification(orderId);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment event: type={}, orderId={}",
                eventType, orderId, e);
            throw new RuntimeException("Failed to process payment event", e);
        }
    }
    
    /**
     * Handles inventory events from the inventory topic.
     */
    @KafkaListener(
        topics = "inventory",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String orderId = null;
        
        try {
            // Parse event wrapper - use a more flexible approach
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> eventMap = objectMapper.readValue(
                eventJson,
                java.util.Map.class
            );
            
            eventType = (String) eventMap.get("eventType");
            
            // Extract orderId from data
            Object dataObj = eventMap.get("data");
            if (dataObj != null && dataObj instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) dataObj;
                orderId = (String) dataMap.get("orderId");
            }
            
            log.info("Received inventory event: type={}, id={}, orderId={}", 
                eventType, eventMap.get("eventId"), orderId);
            
            if ("com.ecommerce.inventory.reserved".equals(eventType)) {
                notificationService.sendInventoryReservedNotification(orderId);
            } else if ("com.ecommerce.inventory.reservation.failed".equals(eventType)) {
                notificationService.sendInventoryReservationFailedNotification(orderId);
            }
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing inventory event: type={}, orderId={}",
                eventType, orderId, e);
            throw new RuntimeException("Failed to process inventory event", e);
        }
    }
    
    /**
     * Handles shipping events from the shipping topic.
     */
    @KafkaListener(
        topics = "shipping",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleShippingEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String eventId = null;
        String orderId = null;
        ShippingLabelCreatedEventData shippingData = null;
        
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
            
            // Parse the data field as ShippingLabelCreatedEventData
            Object dataObj = eventMap.get("data");
            if (dataObj != null) {
                shippingData = objectMapper.convertValue(dataObj, ShippingLabelCreatedEventData.class);
                if (shippingData != null) {
                    orderId = shippingData.getOrderId();
                }
            }
            
            log.info("Received shipping event: type={}, id={}, orderId={}, partition={}, offset={}",
                eventType,
                eventId,
                orderId,
                consumerRecord.partition(),
                consumerRecord.offset());
            
            // Process shipping.label.created or shipping.arranged events
            if (!"com.ecommerce.shipping.label.created".equals(eventType) && 
                !"com.ecommerce.shipping.arranged".equals(eventType)) {
                log.debug("Ignoring event type: {}, expected: com.ecommerce.shipping.label.created or shipping.arranged", eventType);
                acknowledgment.acknowledge();
                return;
            }
            
            if (orderId == null || shippingData == null) {
                log.warn("ShippingLabelCreated event missing orderId or data, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            log.info("Processing order confirmation notification: orderId={}, customerId={}, trackingNumber={}", 
                orderId, shippingData.getCustomerId(), shippingData.getTrackingNumber());
            
            // Send order confirmation notification (with idempotency check using event ID)
            notificationService.sendOrderConfirmation(
                eventId,  // Use event ID for idempotency
                orderId,
                shippingData.getCustomerId(),
                shippingData.getTrackingNumber(),
                shippingData.getCarrier()
            );
            
            log.info("Order confirmation notification processing completed: orderId={}", orderId);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing shipping event: type={}, id={}, orderId={}, eventJson={}",
                eventType, eventId, orderId, eventJson, e);
            // Log the root cause
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage(), e.getCause());
            }
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Failed to process shipping event: " + e.getMessage(), e);
        }
    }
}

