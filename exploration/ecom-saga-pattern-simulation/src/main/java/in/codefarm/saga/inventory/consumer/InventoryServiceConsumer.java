package in.codefarm.saga.inventory.consumer;

import tools.jackson.databind.json.JsonMapper;
import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.PaymentProcessedEvent;
import in.codefarm.saga.event.InventoryReservedEvent;
import in.codefarm.saga.inventory.service.InventoryService;
import in.codefarm.saga.inventory.service.InventoryEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class InventoryServiceConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryServiceConsumer.class);
    
    private final InventoryService inventoryService;
    private final InventoryEventProducer inventoryEventProducer;
    private final JsonMapper jsonMapper;
    
    public InventoryServiceConsumer(
        InventoryService inventoryService,
        InventoryEventProducer inventoryEventProducer,
        @Qualifier("consumerJsonMapper") JsonMapper jsonMapper
    ) {
        this.inventoryService = inventoryService;
        this.inventoryEventProducer = inventoryEventProducer;
        this.jsonMapper = jsonMapper;
    }
    
    @KafkaListener(
        topics = "payments",
        groupId = "inventory-service-group",
        containerFactory = "eventWrapperKafkaListenerContainerFactory"
    )
    public void handlePaymentProcessed(@Payload EventWrapper<?> wrapper) {
        try {
            String eventType = wrapper.metadata().eventType();
            
            // Only process PaymentProcessed events (not PaymentRefunded)
            if (!"PaymentProcessed".equals(eventType)) {
                log.debug("Inventory Service: Ignoring event type: {} - only processing PaymentProcessed events", eventType);
                return;
            }
            
            PaymentProcessedEvent event = jsonMapper.convertValue(wrapper.payload(), PaymentProcessedEvent.class);
            String transactionId = wrapper.metadata().transactionId();
            
            // Only proceed if payment was successful - filter out FAILED events early
            if (!"SUCCESS".equals(event.status())) {
                log.debug("Inventory Service: Payment failed for order: {} - skipping (only processing successful payments)", 
                    event.orderId());
                return;
            }
            
            log.info("Inventory Service: Received PaymentProcessed (SUCCESS) event - OrderId: {}, TransactionId: {}", 
                event.orderId(), transactionId);
            
            // Idempotency check
            var existingReservation = inventoryService.findByOrderId(event.orderId());
            if (existingReservation.isPresent()) {
                log.warn("Inventory already reserved for order: {} - skipping (idempotency)", event.orderId());
                return;
            }
            
            boolean reservationSuccess = reserveInventory(event);
            String reservationId = UUID.randomUUID().toString();
            
            // Save reservation to database
            inventoryService.reserveInventory(
                reservationId,
                event.orderId(),
                "product-123",  // Would come from order
                event.amount().intValue(),  // Simplified
                reservationSuccess ? "RESERVED" : "UNAVAILABLE",
                transactionId
            );
            
            // Publish result
            var inventoryEvent = new InventoryReservedEvent(
                reservationId,
                event.orderId(),
                "product-123",
                event.amount().intValue(),
                reservationSuccess ? "RESERVED" : "UNAVAILABLE",
                LocalDateTime.now()
            );
            
            inventoryEventProducer.sendInventoryReservedEvent(inventoryEvent, transactionId);
            
            log.info("Inventory Service: Inventory reservation - OrderId: {}, Status: {}, TransactionId: {}", 
                event.orderId(), inventoryEvent.status(), transactionId);
            
        } catch (Exception e) {
            log.error("Inventory Service: Error reserving inventory", e);
            
            // Try to extract orderId from wrapper for failure event
            try {
                PaymentProcessedEvent event = jsonMapper.convertValue(wrapper.payload(), PaymentProcessedEvent.class);
                String transactionId = wrapper.metadata().transactionId();
                
                var failureEvent = new InventoryReservedEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    "product-123",
                    0,
                    "UNAVAILABLE",
                    LocalDateTime.now()
                );
                
                inventoryEventProducer.sendInventoryReservedEvent(failureEvent, transactionId);
            } catch (Exception ex) {
                log.error("Failed to send failure event", ex);
            }
        }
    }
    
    private boolean reserveInventory(PaymentProcessedEvent event) {
        // Simulate inventory reservation logic
        // In real system: check stock, reserve items, update database
        
        // Simulate failure: If amount > 500, inventory is unavailable (high demand scenario)
        if (event.amount().compareTo(java.math.BigDecimal.valueOf(500)) > 0) {
            log.warn("Inventory reservation failed: Order amount {} exceeds available inventory capacity (max 500)", 
                event.amount());
            return false;
        }
        
        log.info("Inventory reserved for order: {}", event.orderId());
        return true;
    }
}

