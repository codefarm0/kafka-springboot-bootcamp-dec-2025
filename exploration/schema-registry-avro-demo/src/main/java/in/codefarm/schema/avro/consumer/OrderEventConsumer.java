package in.codefarm.schema.avro.consumer;

import in.codefarm.schema.avro.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Order Event Consumer using Avro deserialization
 * 
 * DEMO NOTES:
 * - Consumes Avro-serialized messages
 * - Schema is automatically fetched from Schema Registry using schema ID
 * - Messages are deserialized to type-safe Avro objects
 * - Supports schema evolution (backward/forward compatibility)
 */
@Component
public class OrderEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);
    
    /**
     * Consume OrderPlacedEvent from Kafka
     * 
     * DEMO NOTES:
     * - Message contains schema ID (4 bytes)
     * - Consumer fetches schema from Schema Registry
     * - Deserializes to OrderPlacedEvent (type-safe)
     * - If schema evolved, compatibility rules ensure it still works
     * 
     * @param event OrderPlacedEvent (Avro-generated class)
     */
    @KafkaListener(
        topics = "orders-avro",
        containerFactory = "avroKafkaListenerContainerFactory",
        groupId = "order-avro-consumer-group"
    )
    public void consume(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent (Avro) - OrderId: {}, CustomerId: {}, ProductId: {}, Quantity: {}, Amount: {}, Date: {}", 
            event.getOrderId(),
            event.getCustomerId(),
            event.getProductId(),
            event.getQuantity(),
            event.getTotalAmount(),
            event.getOrderDate());
        
        // Process the order event
        processOrder(event);
    }
    
    private void processOrder(OrderPlacedEvent event) {
        log.info("Processing order: {}", event.getOrderId());
        // Business logic here
    }
}

