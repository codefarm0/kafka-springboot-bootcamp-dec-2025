package in.codefarm.payment_service.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Dead Letter Topic handler for Payment Service.
 * Processes messages that failed after all retries or were marked as non-retryable.
 */
@Component
@Slf4j
public class PaymentDeadLetterConsumer {
    
    @KafkaListener(
        topics = "orders-dlt",
        groupId = "payment-service-dlt-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetterMessage(
        ConsumerRecord<String, String> consumerRecord,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset,
        @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage,
        @Header(value = KafkaHeaders.EXCEPTION_STACKTRACE, required = false) String stackTrace
    ) {
        String eventJson = consumerRecord.value();
        
        log.error("=== DEAD LETTER MESSAGE RECEIVED (Payment Service) ===");
        log.error("Key: {}, Partition: {}, Offset: {}", key, partition, offset);
        log.error("Exception: {}", exceptionMessage);
        log.error("Stack Trace: {}", stackTrace);
        log.error("Event JSON: {}", eventJson);
        log.error("=======================================================");
        
        // In production, you would typically:
        // 1. Send alert to monitoring system (e.g., PagerDuty, Slack)
        // 2. Store in database for manual review
        // 3. Notify operations team
        // 4. Create support ticket
        // 5. Send metrics to monitoring system
        
        log.error("Dead letter message logged for manual investigation");
    }
}

