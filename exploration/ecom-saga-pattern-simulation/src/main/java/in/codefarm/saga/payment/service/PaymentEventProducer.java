package in.codefarm.saga.payment.service;

import in.codefarm.saga.event.EventMetadata;
import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.PaymentProcessedEvent;
import in.codefarm.saga.event.PaymentRefundedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentEventProducer {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    private static final String PAYMENT_TOPIC = "payments";
    
    private final KafkaTemplate<String, EventWrapper<?>> kafkaTemplate;
    
    public PaymentEventProducer(KafkaTemplate<String, EventWrapper<?>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendPaymentProcessedEvent(PaymentProcessedEvent payload, String transactionId) {
        var metadata = new EventMetadata(
            "PaymentProcessed",
            "1.0",
            "payment-service",
            transactionId,
            LocalDateTime.now()
        );
        
        var event = new EventWrapper<>(metadata, payload);
        
        log.info("Publishing PaymentProcessedEvent - OrderId: {}, Status: {}, TransactionId: {}", 
            payload.orderId(), payload.status(), transactionId);
        
        CompletableFuture<SendResult<String, EventWrapper<?>>> future = 
            kafkaTemplate.send(PAYMENT_TOPIC, payload.orderId(), event);
        
        future.thenAccept(result -> {
            var recordMetadata = result.getRecordMetadata();
            log.info("Payment event sent - OrderId: {}, Partition: {}, Offset: {}", 
                payload.orderId(), recordMetadata.partition(), recordMetadata.offset());
        });
        
        future.exceptionally(ex -> {
            log.error("Failed to send payment event: {}", ex.getMessage(), ex);
            return null;
        });
    }
    
    public void sendPaymentRefundedEvent(PaymentRefundedEvent payload, String transactionId) {
        var metadata = new EventMetadata(
            "PaymentRefunded",
            "1.0",
            "payment-service",
            transactionId,
            LocalDateTime.now()
        );
        
        var event = new EventWrapper<>(metadata, payload);
        
        log.info("Publishing PaymentRefundedEvent - OrderId: {}, TransactionId: {}", 
            payload.orderId(), transactionId);
        
        CompletableFuture<SendResult<String, EventWrapper<?>>> future = 
            kafkaTemplate.send(PAYMENT_TOPIC, payload.orderId(), event);
        
        future.thenAccept(result -> {
            var recordMetadata = result.getRecordMetadata();
            log.info("Payment refund event sent - OrderId: {}, Partition: {}, Offset: {}", 
                payload.orderId(), recordMetadata.partition(), recordMetadata.offset());
        });
        
        future.exceptionally(ex -> {
            log.error("Failed to send payment refund event: {}", ex.getMessage(), ex);
            return null;
        });
    }
}

