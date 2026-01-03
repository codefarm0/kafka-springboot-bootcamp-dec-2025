package in.codefarm.notification.service.as.consumer.consumer;

import in.codefarm.notification.service.as.consumer.event.OrderPlacedEvent;
import in.codefarm.notification.service.as.consumer.exception.OrderIsInvalid;
import in.codefarm.notification.service.as.consumer.exception.TransientDownstreamException;
import in.codefarm.notification.service.as.consumer.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

//@Component
public class NotificationServiceConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceConsumer.class);
    private static final String CONSUMER_GROUP = "notification-service-group";
    
    private final NotificationService notificationService;
    
    public NotificationServiceConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    // Scenario 1: Auto-Commit Consumer (Default)
//    @KafkaListener(
//        topics = "orders",
//        groupId = CONSUMER_GROUP,
//        containerFactory = "kafkaListenerContainerFactory"
//    )
    // Add @RetryableTopic annotation above existing @KafkaListener
//    @RetryableTopic(
//            attempts = "3",  // Retry 3 times
//            backOff = @BackOff(delay = 1000, multiplier = 2.0),  // 1s, 2s, 4s delays
//            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
//            dltStrategy = DltStrategy.FAIL_ON_ERROR,
//            include = {RuntimeException.class}  // Retry on runtime exceptions
//    )
    public void consumeAutoCommit(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) throws Throwable{
        log.info("=== Auto-Commit Consumer: Received OrderPlacedEvent ===");
        log.info("Order ID: {}, Customer: {}, Partition: {}, Offset: {}", 
            event.orderId(), event.customerId(), partition, offset);
        
        try {
            // Send notification
            notificationService.sendEmailNotification(event);
            
            // Save to database
            notificationService.saveNotification(
                event,
                "EMAIL",
                "SENT",
                "Order confirmation email sent",
                partition,
                offset,
                CONSUMER_GROUP,
                "auto-commit"
            );
            
            log.info("=== Auto-Commit Consumer: Notification processed successfully for order {} ===", 
                event.orderId());
            
        } catch (Exception e) {
            log.error("=== Auto-Commit Consumer: Error processing order {} ===", event.orderId(), e);
            throw e;
        }
    }
    
    // Scenario 2: Manual Commit Consumer
    @KafkaListener(
        topics = "orders",
        groupId = "notification-service-manual-group",
        containerFactory = "manualCommitKafkaListenerContainerFactory"
    )
    public void consumeManualCommit(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        Acknowledgment acknowledgment
    ) throws Throwable{
        log.info("=== Manual Commit Consumer: Received OrderPlacedEvent ===");
        log.info("Order ID: {}, Customer: {}, Partition: {}, Offset: {}", 
            event.orderId(), event.customerId(), partition, offset);
        
        try {
            // Send notification
            notificationService.sendEmailNotification(event);
            
            // Save to database
            notificationService.saveNotification(
                event,
                "EMAIL",
                "SENT",
                "Order confirmation email sent (manual commit)",
                partition,
                offset,
                "notification-service-manual-group",
                "manual-commit"
            );
            
            // Explicitly commit offset after successful processing
            acknowledgment.acknowledge();
            
            log.info("=== Manual Commit Consumer: Order processed and offset committed - Offset: {} ===", 
                offset);
            
        } catch (Exception e) {
            log.error("=== Manual Commit Consumer: Error processing order {} ===", event.orderId(), e);
            // Don't acknowledge - message will be reprocessed
            throw e;
        }
    }
    
    // Scenario 3: Batch Consumption
//    @KafkaListener(
//        topics = "orders",
//        groupId = "notification-service-batch-group",
//        containerFactory = "batchKafkaListenerContainerFactory"
//    )
    public void consumeBatch(
        @Payload List<OrderPlacedEvent> events,
        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
        @Header(KafkaHeaders.OFFSET) List<Long> offsets
    ) throws Throwable{
        log.info("=== Batch Consumer: Received batch of {} orders ===", events.size());
        
        for (int i = 0; i < events.size(); i++) {
            OrderPlacedEvent event = events.get(i);
            int partition = partitions.get(i);
            long offset = offsets.get(i);
            
            log.info("=== Batch Consumer: Processing order {} from partition {}, offset {} ===",
                event.orderId(), partition, offset);
            
            try {
                // Send notification
                notificationService.sendEmailNotification(event);
                
                // Save to database
                notificationService.saveNotification(
                    event,
                    "EMAIL",
                    "SENT",
                    "Order confirmation email sent (batch)",
                    partition,
                    offset,
                    "notification-service-batch-group",
                    "batch"
                );
                
            } catch (Exception e) {
                log.error("=== Batch Consumer: Error processing order {} in batch ===", event.orderId(), e);
            }
        }
        
        log.info("=== Batch Consumer: Batch processing complete for {} orders ===", events.size());
    }
    
    // Scenario 4: Consume from Specific Partitions
    @KafkaListener(
        topicPartitions = @TopicPartition(
            topic = "orders",
            partitions = {"3"} //US  // Only consume from partitions 0 and 1
        ),
        groupId = "notification-service-specific-partition-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeFromSpecificPartitions(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) throws Throwable{
        log.info("=== Specific Partition Consumer: Received from partition {} - Order: {} ===",
            partition, event.orderId());
        
        try {
            notificationService.sendEmailNotification(event);
            
            notificationService.saveNotification(
                event,
                "EMAIL",
                "SENT",
                "Order confirmation email sent (specific partition)",
                partition,
                offset,
                "notification-service-specific-partition-group",
                "specific-partition"
            );
            
        } catch (Exception e) {
            log.error("=== Specific Partition Consumer: Error processing order {} ===", event.orderId(), e);
            throw e;
        }
    }
    
    // Scenario 5: Reading Headers from Messages
//    @KafkaListener(
//        topics = "orders",
//        groupId = "notification-service-headers-group",
//        containerFactory = "kafkaListenerContainerFactory"
//    )
    public void consumeWithHeaders(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        @Header(value = "correlation-id", required = false) String correlationId,
        @Header(value = "source", required = false) String source,
        @Header(value = "event-version", required = false) String eventVersion
    ) throws Throwable{
        log.info("=== Headers Consumer: Received OrderPlacedEvent: {} ===", event.orderId());
        log.info("=== Headers Consumer: Correlation ID: {}, Source: {}, Event Version: {} ===",
            correlationId, source, eventVersion);
        
        try {
            notificationService.sendEmailNotification(event);
            
            String message = String.format("Order confirmation email sent (with headers - correlation-id: %s)",
                correlationId != null ? correlationId : "N/A");
            
            notificationService.saveNotification(
                event,
                "EMAIL",
                "SENT",
                message,
                partition,
                offset,
                "notification-service-headers-group",
                "with-headers"
            );
            
        } catch (Exception e) {
            log.error("=== Headers Consumer: Error processing order {} ===", event.orderId(), e);
            throw e;
        }
    }
    
    // Scenario 5b: Read All Headers
//    @KafkaListener(
//        topics = "orders",
//        groupId = "notification-service-all-headers-group",
//        containerFactory = "kafkaListenerContainerFactory"
//    )
    public void consumeWithAllHeaders(
        @Payload OrderPlacedEvent event,
        @Header Map<String, Object> headers
    ) throws Throwable{
        log.info("=== All Headers Consumer: Received OrderPlacedEvent: {} ===", event.orderId());
        log.info("=== All Headers Consumer: All headers: {} ===", headers);
        
        headers.forEach((key, value) -> {
            log.info("=== All Headers Consumer: Header - {}: {} ===", key, value);
        });
        
        try {
            notificationService.sendEmailNotification(event);
            
            notificationService.saveNotification(
                event,
                "EMAIL",
                "SENT",
                "Order confirmation email sent (all headers)",
                null,
                null,
                "notification-service-all-headers-group",
                "all-headers"
            );
            
        } catch (Exception e) {
            log.error("=== All Headers Consumer: Error processing order {} ===", event.orderId(), e);
            throw e;
        }
    }
    
    // Scenario 6: Concurrent Consumers
//    @KafkaListener(
//        topics = "orders",
//        groupId = "notification-service-concurrent-group",
//        containerFactory = "kafkaListenerContainerFactory"
//    )
    public void consumeConcurrently(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition
    ) throws Throwable{
        log.info("=== Concurrent Consumer: Thread: {}, Partition: {}, Order: {} ===",
            Thread.currentThread().getName(),
            partition,
            event.orderId());
        
        try {
            notificationService.sendEmailNotification(event);
            
            notificationService.saveNotification(
                event,
                "EMAIL",
                "SENT",
                "Order confirmation email sent (concurrent)",
                partition,
                null,
                "notification-service-concurrent-group",
                "concurrent"
            );
            
        } catch (Exception e) {
            log.error("=== Concurrent Consumer: Error processing order {} ===", event.orderId(), e);
            throw e;
        }
    }
    
    // Scenario 7: Idempotent Consumer
//    @KafkaListener(
//        topics = "orders",
//        groupId = "notification-service-idempotent-group",
//        containerFactory = "kafkaListenerContainerFactory"
//    )
    public void consumeIdempotently(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.OFFSET) long offset
    ) throws Throwable{
        log.info("=== Idempotent Consumer: Received OrderPlacedEvent: {} ===", event.orderId());
        
        // Idempotency check - check if notification already sent for this order
        var existingNotification = notificationService.findByOrderId(event.orderId());
        
        if (existingNotification.isPresent()) {
            log.warn("=== Idempotent Consumer: Order {} already processed - skipping (idempotency) ===",
                event.orderId());
            return; // Skip duplicate
        }
        
        try {
            notificationService.sendEmailNotification(event);
            
            notificationService.saveNotification(
                event,
                "EMAIL",
                "SENT",
                "Order confirmation email sent (idempotent)",
                null,
                offset,
                "notification-service-idempotent-group",
                "idempotent"
            );
            
            log.info("=== Idempotent Consumer: Order {} processed successfully ===", event.orderId());
            
        } catch (Exception e) {
            log.error("=== Idempotent Consumer: Error processing order {} ===", event.orderId(), e);
            throw e;
        }
    }
}

