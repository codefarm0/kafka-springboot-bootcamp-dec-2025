package in.codefarm.order_service.consumer;

import in.codefarm.order_service.entity.Order;
import in.codefarm.order_service.service.OrderService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Event consumer for saga orchestration.
 * Listens to events from Payment, Inventory, and Shipping services
 * to update order status accordingly.
 * 
 * Parses JSON string events and filters by eventType inside the handlers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    
    /**
     * Handles payment events from the payments topic.
     * Filters by eventType to handle different payment event types.
     */
    @KafkaListener(
        topics = "payments",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String eventId = null;
        String orderId = null;
        
        try {
            // Parse event wrapper - use a flexible approach
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> eventMap = objectMapper.readValue(
                eventJson,
                java.util.Map.class
            );
            
            eventType = (String) eventMap.get("eventType");
            eventId = (String) eventMap.get("eventId");
            
            // Extract orderId from the data field
            Object dataObj = eventMap.get("data");
            if (dataObj != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = objectMapper.convertValue(dataObj, java.util.Map.class);
                orderId = (String) dataMap.get("orderId");
            }
            
            log.info("Received payment event: type={}, id={}, orderId={}, partition={}, offset={}",
                eventType,
                eventId,
                orderId,
                consumerRecord.partition(),
                consumerRecord.offset());
            
            if (orderId == null) {
                log.warn("Payment event missing orderId, skipping: type={}, id={}",
                    eventType, eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            switch (eventType) {
                case "com.ecommerce.payment.processed":
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.PAYMENT_COMPLETED);
                    log.info("Order status updated to PAYMENT_COMPLETED: orderId={}", orderId);
                    break;
                    
                case "com.ecommerce.payment.failed":
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.PAYMENT_FAILED);
                    log.info("Order status updated to PAYMENT_FAILED: orderId={}", orderId);
                    break;
                    
                default:
                    log.warn("Unknown payment event type: {}, id={}, orderId={}",
                        eventType, eventId, orderId);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing payment event: type={}, id={}, orderId={}, eventJson={}",
                eventType, eventId, orderId, eventJson, e);
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Failed to process payment event: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handles inventory events from the inventory topic.
     * Filters by eventType to handle different inventory event types.
     */
    @KafkaListener(
        topics = "inventory",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleInventoryEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String eventId = null;
        String orderId = null;
        
        try {
            // Parse event wrapper - use a flexible approach
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> eventMap = objectMapper.readValue(
                eventJson,
                java.util.Map.class
            );
            
            eventType = (String) eventMap.get("eventType");
            eventId = (String) eventMap.get("eventId");
            
            // Extract orderId from the data field
            Object dataObj = eventMap.get("data");
            if (dataObj != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = objectMapper.convertValue(dataObj, java.util.Map.class);
                orderId = (String) dataMap.get("orderId");
            }
            
            log.info("Received inventory event: type={}, id={}, orderId={}, partition={}, offset={}",
                eventType,
                eventId,
                orderId,
                consumerRecord.partition(),
                consumerRecord.offset());
            
            if (orderId == null) {
                log.warn("Inventory event missing orderId, skipping: type={}, id={}",
                    eventType, eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            switch (eventType) {
                case "com.ecommerce.inventory.reserved":
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.INVENTORY_RESERVED);
                    log.info("Order status updated to INVENTORY_RESERVED: orderId={}", orderId);
                    break;
                    
                case "com.ecommerce.inventory.reservation.failed":
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.INVENTORY_RESERVATION_FAILED);
                    log.info("Order status updated to INVENTORY_RESERVATION_FAILED: orderId={}", orderId);
                    break;
                    
                default:
                    log.warn("Unknown inventory event type: {}, id={}, orderId={}",
                        eventType, eventId, orderId);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing inventory event: type={}, id={}, orderId={}, eventJson={}",
                eventType, eventId, orderId, eventJson, e);
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Failed to process inventory event: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handles shipping events from the shipping topic.
     * Filters by eventType to handle different shipping event types.
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
        
        try {
            // Parse event wrapper - use a flexible approach
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> eventMap = objectMapper.readValue(
                eventJson,
                java.util.Map.class
            );
            
            eventType = (String) eventMap.get("eventType");
            eventId = (String) eventMap.get("eventId");
            
            // Extract orderId from the data field
            Object dataObj = eventMap.get("data");
            if (dataObj != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = objectMapper.convertValue(dataObj, java.util.Map.class);
                orderId = (String) dataMap.get("orderId");
            }
            
            log.info("Received shipping event: type={}, id={}, orderId={}, partition={}, offset={}",
                eventType,
                eventId,
                orderId,
                consumerRecord.partition(),
                consumerRecord.offset());
            
            if (orderId == null) {
                log.warn("Shipping event missing orderId, skipping: type={}, id={}",
                    eventType, eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            switch (eventType) {
                case "com.ecommerce.shipping.arranged":
                case "com.ecommerce.shipping.label.created":
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.SHIPPING_ARRANGED);
                    // After shipping is arranged, order is completed
                    // TODO: Add logic to check if all items are shipped, 
                    // after shipping arrangment there will be regular updates from shipping company completed the delivery
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.COMPLETED);
                    log.info("Order status updated to COMPLETED: orderId={}", orderId);
                    break;
                    
                case "com.ecommerce.shipping.failed":
                    orderService.updateOrderStatus(orderId, Order.OrderStatus.SHIPPING_FAILED);
                    log.info("Order status updated to SHIPPING_FAILED: orderId={}", orderId);
                    break;
                    
                default:
                    log.warn("Unknown shipping event type: {}, id={}, orderId={}",
                        eventType, eventId, orderId);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing shipping event: type={}, id={}, orderId={}, eventJson={}",
                eventType, eventId, orderId, eventJson, e);
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Failed to process shipping event: " + e.getMessage(), e);
        }
    }
}
