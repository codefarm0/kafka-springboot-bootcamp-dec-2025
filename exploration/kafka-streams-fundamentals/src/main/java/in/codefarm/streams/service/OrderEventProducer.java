package in.codefarm.streams.service;

import in.codefarm.streams.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class OrderEventProducer {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    private static final String ORDERS_TOPIC = "orders";
    
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    
    public OrderEventProducer(KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Publishing OrderPlacedEvent - OrderId: {}, Amount: {}", 
            event.orderId(), event.totalAmount());
        
        CompletableFuture<SendResult<String, OrderPlacedEvent>> future = 
            kafkaTemplate.send(ORDERS_TOPIC, event.orderId(), event);
        
        future.thenAccept(result -> {
            var metadata = result.getRecordMetadata();
            log.info("Order event sent - OrderId: {}, Partition: {}, Offset: {}", 
                event.orderId(), metadata.partition(), metadata.offset());
        });
        
        future.exceptionally(ex -> {
            log.error("Failed to send order event: {}", ex.getMessage(), ex);
            return null;
        });
    }
}

