package in.codefarm.saga.order.service;

import in.codefarm.saga.event.EventMetadata;
import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.OrderPlacedEvent;
import in.codefarm.saga.event.OrderCancelledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class OrderEventProducer {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);
    private static final String ORDERS_TOPIC = "orders";
    
    private final KafkaTemplate<String, EventWrapper<?>> kafkaTemplate;
    
    public OrderEventProducer(KafkaTemplate<String, EventWrapper<?>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendOrderPlacedEvent(OrderPlacedEvent payload, String transactionId) {
        var metadata = new EventMetadata(
            "OrderPlaced",
            "1.0",
            "order-service",
            transactionId,
            LocalDateTime.now()
        );
        
        var event = new EventWrapper<>(metadata, payload);
        
        log.info("Publishing OrderPlacedEvent - OrderId: {}, TransactionId: {}", 
            payload.orderId(), transactionId);
        
        CompletableFuture<SendResult<String, EventWrapper<?>>> future = 
            kafkaTemplate.send(ORDERS_TOPIC, payload.orderId(), event);
        
        future.thenAccept(result -> {
            var recordMetadata = result.getRecordMetadata();
            log.info("Order event sent - OrderId: {}, Partition: {}, Offset: {}", 
                payload.orderId(), recordMetadata.partition(), recordMetadata.offset());
        });
        
        future.exceptionally(ex -> {
            log.error("Failed to send order event: {}", ex.getMessage(), ex);
            return null;
        });
    }
    
    public void sendOrderCancelledEvent(OrderCancelledEvent payload, String transactionId) {
        var metadata = new EventMetadata(
            "OrderCancelled",
            "1.0",
            "order-service",
            transactionId,
            LocalDateTime.now()
        );
        
        var event = new EventWrapper<>(metadata, payload);
        
        log.info("Publishing OrderCancelledEvent - OrderId: {}, TransactionId: {}", 
            payload.orderId(), transactionId);
        
        CompletableFuture<SendResult<String, EventWrapper<?>>> future = 
            kafkaTemplate.send(ORDERS_TOPIC, payload.orderId(), event);
        
        future.thenAccept(result -> {
            var recordMetadata = result.getRecordMetadata();
            log.info("Order cancelled event sent - OrderId: {}, Partition: {}, Offset: {}", 
                payload.orderId(), recordMetadata.partition(), recordMetadata.offset());
        });
        
        future.exceptionally(ex -> {
            log.error("Failed to send order cancelled event: {}", ex.getMessage(), ex);
            return null;
        });
    }
}

