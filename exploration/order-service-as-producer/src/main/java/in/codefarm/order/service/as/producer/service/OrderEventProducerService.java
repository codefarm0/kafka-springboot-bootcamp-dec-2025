package in.codefarm.order.service.as.producer.service;

import in.codefarm.order.service.as.producer.entity.OrderEntity;
import in.codefarm.order.service.as.producer.event.OrderPlacedEvent;
import in.codefarm.order.service.as.producer.repository.OrderRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OrderEventProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventProducerService.class);
    private static final String TOPIC_NAME = "orders";

//    @Qualifier("kafkaTemplate1")
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final OrderRepository orderRepository;
    
    public OrderEventProducerService(
        KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate,
        OrderRepository orderRepository
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }
    
    // Scenario 1: Fire-and-Forget (Async, No Wait)
    @Transactional
    public OrderEntity fireAndForget(OrderPlacedEvent event) {
        log.info("=== Fire-and-Forget: Sending order event {} ===", event.orderId());
        
        // Save to database
        var orderEntity = saveOrderToDatabase(event);
        
        // Send and don't wait - fire and forget
        kafkaTemplate.send(TOPIC_NAME, event.orderId(), event);
        
        log.info("=== Fire-and-Forget: Message sent (no confirmation) for order {} ===", event.orderId());
        return orderEntity;
    }
    
    // Scenario 2: Synchronous Send (Wait for Result)
    @Transactional
    public OrderEntity sendSynchronously(OrderPlacedEvent event) throws Exception {
        log.info("=== Synchronous Send: Sending order event {} ===", event.orderId());
        
        // Save to database
        var orderEntity = saveOrderToDatabase(event);
        
        try {
            // Send and wait for result (blocks until complete)
            SendResult<String, OrderPlacedEvent> result = kafkaTemplate
                .send(TOPIC_NAME, event.orderId(), event)
                .get(); // .get() blocks until completion
            
            var metadata = result.getRecordMetadata();
            log.info("=== Synchronous Send: Message sent successfully - Topic: {}, Partition: {}, Offset: {} ===",
                metadata.topic(),
                metadata.partition(),
                metadata.offset());
            
            return orderEntity;
            
        } catch (ExecutionException e) {
            log.error("=== Synchronous Send: Failed to send message for order {} ===", event.orderId(), e);
            throw new Exception("Failed to send message", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("=== Synchronous Send: Interrupted while sending message for order {} ===", event.orderId(), e);
            throw new Exception("Interrupted", e);
        }
    }
    
    // Scenario 2b: Synchronous Send with Timeout
    @Transactional
    public OrderEntity sendSynchronouslyWithTimeout(OrderPlacedEvent event, long timeoutSeconds) throws Exception {
        log.info("=== Synchronous Send (Timeout {}s): Sending order event {} ===", timeoutSeconds, event.orderId());
        
        // Save to database
        var orderEntity = saveOrderToDatabase(event);
        
        try {
            SendResult<String, OrderPlacedEvent> result = kafkaTemplate
                .send(TOPIC_NAME, event.orderId(), event)
                .get(timeoutSeconds, TimeUnit.SECONDS); // Wait max timeoutSeconds
            
            log.info("=== Synchronous Send (Timeout): Message sent - Offset: {} ===",
                result.getRecordMetadata().offset());
            
            return orderEntity;
            
        } catch (TimeoutException e) {
            log.error("=== Synchronous Send (Timeout): Timeout sending message after {} seconds for order {} ===",
                timeoutSeconds, event.orderId());
            throw new Exception("Timeout sending message", e);
        } catch (ExecutionException | InterruptedException e) {
            log.error("=== Synchronous Send (Timeout): Error sending message for order {} ===", event.orderId(), e);
            throw new Exception("Error sending message", e);
        }
    }
    
    // Scenario 3: Async with Callback (Recommended)
    @Transactional
    public OrderEntity sendWithCallback(OrderPlacedEvent event) {
        log.info("=== Async with Callback: Sending order event {} ===", event.orderId());
        
        // Save to database
        var orderEntity = saveOrderToDatabase(event);
        
        CompletableFuture<SendResult<String, OrderPlacedEvent>> future = 
            kafkaTemplate.send(TOPIC_NAME, event.orderId(), event);
        
        // Handle success
        future.thenAccept(result -> {
            var metadata = result.getRecordMetadata();
            log.info("=== Async with Callback: Message sent successfully - Topic: {}, Partition: {}, Offset: {} ===",
                metadata.topic(),
                metadata.partition(),
                metadata.offset());
        });
        
        // Handle failure
        future.exceptionally(ex -> {
            log.error("=== Async with Callback: Failed to send message for order {} ===", event.orderId(), ex);
            return null;
        });
        
        log.info("=== Async with Callback: Request processed, callback will handle result for order {} ===", event.orderId());
        return orderEntity;
    }
    
    // Scenario 4: Send to Specific Partition
    //todo how to get the parittion numbers from the topic from producer client
    @Transactional
    public OrderEntity sendToPartition(OrderPlacedEvent event, int partition) {
        log.info("=== Send to Partition {}: Sending order event {} ===", partition, event.orderId());
        
        // Save to database
        var orderEntity = saveOrderToDatabase(event);
        
        // Send to specific partition
        CompletableFuture<SendResult<String, OrderPlacedEvent>> future = 
            kafkaTemplate.send(TOPIC_NAME, partition, event.orderId(), event);
        
        future.thenAccept(result -> {
            var metadata = result.getRecordMetadata();
            log.info("=== Send to Partition {}: Sent to partition {} - Offset: {} ===",
                partition,
                metadata.partition(),
                metadata.offset());
        });
        
        future.exceptionally(ex -> {
            log.error("=== Send to Partition {}: Failed to send message for order {} ===",
                partition, event.orderId(), ex);
            return null;
        });
        
        return orderEntity;
    }
    
    // Scenario 5: Send with Custom Headers, this is the recommended way to use from my side
    @Transactional
    public OrderEntity sendWithHeaders(OrderPlacedEvent event) {
        log.info("=== Send with Headers: Sending order event {} ===", event.orderId());
        
        // Save to database
        var orderEntity = saveOrderToDatabase(event);
        
        // Create ProducerRecord with headers
        ProducerRecord<String, OrderPlacedEvent> record = new ProducerRecord<>(
            TOPIC_NAME,
            event.orderId(),
            event
        );
        
        // Add headers
        record.headers().add("correlation-id", event.orderId().getBytes());
        record.headers().add("source", "order-service".getBytes());
        record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
        record.headers().add("event-version", "1.0".getBytes());
        
        log.info("=== Send with Headers: Added headers - correlation-id: {}, source: order-service ===",
            event.orderId());
        
        // Send
        CompletableFuture<SendResult<String, OrderPlacedEvent>> future = 
            kafkaTemplate.send(record);
        
        future.thenAccept(result -> {
            log.info("=== Send with Headers: Message sent with headers - Offset: {} ===",
                result.getRecordMetadata().offset());
        });
        
        future.exceptionally(ex -> {
            log.error("=== Send with Headers: Failed to send message for order {} ===", event.orderId(), ex);
            return null;
        });
        
        return orderEntity;
    }
    
    // Scenario 5b: Send with Spring Message API
    @Transactional
    public OrderEntity sendWithSpringMessage(OrderPlacedEvent event) {
        log.info("=== Send with Spring Message: Sending order event {} ===", event.orderId());
        
        // Save to database
        var orderEntity = saveOrderToDatabase(event);
        
        Message<OrderPlacedEvent> message = MessageBuilder
            .withPayload(event)
            .setHeader(KafkaHeaders.TOPIC, TOPIC_NAME)
            .setHeader(KafkaHeaders.KEY, event.orderId())
            .setHeader("correlation-id", event.orderId())
            .setHeader("source", "order-service")
            .setHeader("message-type", "OrderPlacedEvent")
            .build();
        
        log.info("=== Send with Spring Message: Built message with headers ===");
        
        kafkaTemplate.send(message);
        
        log.info("=== Send with Spring Message: Message sent for order {} ===", event.orderId());
        return orderEntity;
    }
    
    // Scenario 6: Send with Timestamp
    @Transactional
    public OrderEntity sendWithTimestamp(OrderPlacedEvent event) {
        log.info("=== Send with Timestamp: Sending order event {} with timestamp {} ===",
            event.orderId(), event.orderDate());
        
        // Save to database
        var orderEntity = saveOrderToDatabase(event);
        
        // Create record with specific timestamp
        ProducerRecord<String, OrderPlacedEvent> record = new ProducerRecord<>(
            TOPIC_NAME,
            null, // partition (let Kafka decide)
            event.orderDate().toEpochSecond(ZoneOffset.UTC) * 1000, // timestamp in milliseconds
            event.orderId(),
            event
        );
        
        log.info("=== Send with Timestamp: Using event timestamp: {} ===", event.orderDate());
        
        kafkaTemplate.send(record);
        
        log.info("=== Send with Timestamp: Message sent with custom timestamp for order {} ===", event.orderId());
        return orderEntity;
    }
    
    // Scenario 7: Batch Sending
    @Transactional
    public List<OrderEntity> sendBatch(List<OrderPlacedEvent> events) {
        log.info("=== Batch Send: Sending {} order events ===", events.size());
        
        List<OrderEntity> orderEntities = new ArrayList<>();
        List<CompletableFuture<SendResult<String, OrderPlacedEvent>>> futures = new ArrayList<>();
        
        // Save all to database
        for (OrderPlacedEvent event : events) {
            var orderEntity = saveOrderToDatabase(event);
            orderEntities.add(orderEntity);
        }
        
        log.info("=== Batch Send: Saved {} orders to database ===", orderEntities.size());
        
        // Send all messages
        for (OrderPlacedEvent event : events) {
            CompletableFuture<SendResult<String, OrderPlacedEvent>> future = 
                kafkaTemplate.send(TOPIC_NAME, event.orderId(), event);
            futures.add(future);
        }
        
        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                log.info("=== Batch Send: All {} messages sent successfully ===", events.size());
            })
            .exceptionally(ex -> {
                log.error("=== Batch Send: Some messages failed in batch ===", ex);
                return null;
            });
        
        log.info("=== Batch Send: Batch processing initiated for {} orders ===", events.size());
        return orderEntities;
    }
    
    // Scenario 7b: Batch Send Synchronously
    @Transactional
    public List<OrderEntity> sendBatchSynchronously(List<OrderPlacedEvent> events) throws Exception {
        log.info("=== Batch Send Synchronous: Sending {} order events ===", events.size());
        
        List<OrderEntity> orderEntities = new ArrayList<>();
        List<SendResult<String, OrderPlacedEvent>> results = new ArrayList<>();
        
        // Save all to database
        for (OrderPlacedEvent event : events) {
            var orderEntity = saveOrderToDatabase(event);
            orderEntities.add(orderEntity);
        }
        
        log.info("=== Batch Send Synchronous: Saved {} orders to database ===", orderEntities.size());
        
        // Send all messages synchronously
        for (OrderPlacedEvent event : events) {
            try {
                SendResult<String, OrderPlacedEvent> result = kafkaTemplate
                    .send(TOPIC_NAME, event.orderId(), event)
                    .get();
                results.add(result);
                log.info("=== Batch Send Synchronous: Sent order {} - Partition: {}, Offset: {} ===",
                    event.orderId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } catch (Exception e) {
                log.error("=== Batch Send Synchronous: Failed to send event {} in batch ===",
                    event.orderId(), e);
                throw e;
            }
        }
        
        log.info("=== Batch Send Synchronous: All {} messages sent successfully ===", events.size());
        return orderEntities;
    }
    
    // Helper method to save order to database
    private OrderEntity saveOrderToDatabase(OrderPlacedEvent event) {
        log.debug("Saving order {} to database", event.orderId());
        
        var orderEntity = new OrderEntity(
            event.orderId(),
            event.customerId(),
            event.productId(),
            event.quantity(),
            event.totalAmount(),
            event.orderDate()
        );
        
        var saved = orderRepository.save(orderEntity);
        log.info("Order {} saved to database with ID: {}", event.orderId(), saved.getId());
        
        return saved;
    }
}

