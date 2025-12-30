package in.codefarm.order.service.as.producer.service;

import in.codefarm.order.service.as.producer.event.OrderPlacedEvent;
import in.codefarm.order.service.as.producer.event.PaymentProcessedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentEventProducer {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);
    private static final String PAYMENT_TOPIC = "payments";
    private static final String ORDER_TOPIC = "orders";
    
    private final KafkaTemplate<String, Object> transactionalKafkaTemplate;
    private final KafkaTemplate<String, Object> nonTransactionalKafkaTemplate;
    
    public PaymentEventProducer(
        @Qualifier("transactionalKafkaTemplate") KafkaTemplate<String, Object> transactionalKafkaTemplate,
        @Qualifier("nonTransactionalKafkaTemplate") KafkaTemplate<String, Object> nonTransactionalKafkaTemplate
    ) {
        this.transactionalKafkaTemplate = transactionalKafkaTemplate;
        this.nonTransactionalKafkaTemplate = nonTransactionalKafkaTemplate;

        // Log transactional ID if available
        if (transactionalKafkaTemplate.getProducerFactory() instanceof DefaultKafkaProducerFactory) {
            var factory = (DefaultKafkaProducerFactory<?, ?>) transactionalKafkaTemplate.getProducerFactory();
            var config = factory.getConfigurationProperties();
            var transactionalId = config.get(ProducerConfig.TRANSACTIONAL_ID_CONFIG);
            log.info("Transactional Producer ID: {}", transactionalId);
        }
    }
    
    @Transactional("kafkaTransactionManager")
    public String processPaymentTransactionally(
        String orderId,
        String customerId,
        java.math.BigDecimal amount
    ) {
        String transactionId = UUID.randomUUID().toString();
        processPayment(orderId, customerId, amount, transactionId, transactionalKafkaTemplate);
        return transactionId;
    }
    
    public String processPaymentNonTransactionally(
        String orderId,
        String customerId,
        java.math.BigDecimal amount
    ) {
        String transactionId = UUID.randomUUID().toString();
        processPayment(orderId, customerId, amount, transactionId, nonTransactionalKafkaTemplate);
        return transactionId;
    }
    
    private void processPayment(
        String orderId,
        String customerId,
        java.math.BigDecimal amount,
        String transactionId,
        KafkaTemplate<String, Object> kafkaTemplate
    ) {
        // Validation
        if (amount.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Invalid amount - must be positive");
        }

        
        // Create payment event
        var paymentEvent = new PaymentProcessedEvent(
            UUID.randomUUID().toString(),
            orderId,
            customerId,
            amount,
            "PROCESSED",
            java.time.LocalDateTime.now(),
            transactionId
        );
        
        // Send payment event
        CompletableFuture<SendResult<String, Object>> paymentFuture = kafkaTemplate.send(
            PAYMENT_TOPIC, 
            paymentEvent.paymentId(), 
            paymentEvent
        );
        
        paymentFuture.thenAccept(result -> {
            log.debug("Payment event sent - PaymentId: {}, TransactionId: {}, Partition: {}", 
                paymentEvent.paymentId(), transactionId, result.getRecordMetadata().partition());
        });
        
        // Create order event
        var orderEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            "product-123",
            1,
            amount,
            java.time.LocalDateTime.now(),
            transactionId
        );

        // Simulate failure for large amounts (non-transactional will show inconsistency)
        if (amount.compareTo(new java.math.BigDecimal("1000")) > 0) {
            throw new RuntimeException("Amount too large - order event failed");
        }

        // Send order event
        CompletableFuture<SendResult<String, Object>> orderFuture = kafkaTemplate.send(
            ORDER_TOPIC, 
            orderId, 
            orderEvent
        );
        
        orderFuture.thenAccept(result -> {
            log.debug("Order event sent - OrderId: {}, TransactionId: {}, Partition: {}", 
                orderId, transactionId, result.getRecordMetadata().partition());
        });
        
        log.info("Payment processing completed - OrderId: {}, TransactionId: {}", orderId, transactionId);
    }
}

