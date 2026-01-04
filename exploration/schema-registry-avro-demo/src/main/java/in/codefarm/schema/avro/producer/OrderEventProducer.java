package in.codefarm.schema.avro.producer;

import in.codefarm.schema.avro.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Order Event Producer using Avro serialization
 * 
 * DEMO NOTES:
 * - Produces Avro-serialized messages
 * - Schema is automatically registered with Schema Registry on first send
 * - Messages are binary-encoded (smaller than JSON)
 * - Type-safe serialization
 */
@Service
public class OrderEventProducer {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    private static final String ORDERS_TOPIC = "orders-avro";


    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    
    public OrderEventProducer( @Qualifier("avroKafkaTemplate")KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Send OrderPlacedEvent using Avro serialization
     * 
     * DEMO NOTES:
     * - Event is serialized to Avro binary format
     * - Schema ID is embedded in message (4 bytes)
     * - Schema Registry is automatically contacted to register/fetch schema
     * 
     * @param event OrderPlacedEvent (Avro-generated class)
     */
    public void sendOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Publishing OrderPlacedEvent (Avro) - OrderId: {}, CustomerId: {}, Amount: {}", 
            event.getOrderId(), event.getCustomerId(), event.getTotalAmount());
        
        CompletableFuture<SendResult<String, OrderPlacedEvent>> future = 
            kafkaTemplate.send(ORDERS_TOPIC, event.getOrderId(), event);
        
        future.thenAccept(result -> {
            var recordMetadata = result.getRecordMetadata();
            log.info("Order event sent (Avro) - OrderId: {}, Partition: {}, Offset: {}, Schema ID: {}", 
                event.getOrderId(), 
                recordMetadata.partition(), 
                recordMetadata.offset(),
                "Embedded in message");
        });
        
        future.exceptionally(ex -> {
            log.error("Failed to send order event (Avro): {}", ex.getMessage(), ex);
            return null;
        });
    }
}

