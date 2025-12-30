package in.codefarm.notification.service.as.consumer.consumer;

import in.codefarm.notification.service.as.consumer.event.OrderPlacedEvent;
import in.codefarm.notification.service.as.consumer.event.PaymentProcessedEvent;
import in.codefarm.notification.service.as.consumer.service.NotificationService;
import in.codefarm.notification.service.as.consumer.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class PaymentServiceConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceConsumer.class);
    private static final String ORDER_CONSUMER_GROUP = "payment-service-order-group";
    private static final String PAYMENT_CONSUMER_GROUP = "payment-service-payment-group";
    
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    
    public PaymentServiceConsumer(
        NotificationService notificationService,
        PaymentService paymentService
    ) {
        this.notificationService = notificationService;
        this.paymentService = paymentService;
    }
    
    /**
     * Consumer for OrderPlacedEvent - Idempotent processing
     * This consumer demonstrates idempotent processing to handle duplicates gracefully
     */
    @KafkaListener(
        topics = "orders",
        groupId = ORDER_CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderPlacedEvent(
        @Payload OrderPlacedEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Order received - OrderId: {}, TransactionId: {}, Customer: {}", 
            event.orderId(), event.transactionId(), event.customerId());
        
        // Idempotency check - check if already processed
        var existingNotification = notificationService.findByOrderId(event.orderId());
        if (existingNotification.isPresent()) {
            log.warn("Order {} already processed - skipping (idempotency). TransactionId: {}", 
                event.orderId(), event.transactionId());
            return;
        }
        
        try {
            notificationService.sendEmailNotification(event);
            notificationService.saveNotification(
                event,
                "EMAIL",
                "SENT",
                "Order confirmation email sent",
                partition,
                offset,
                ORDER_CONSUMER_GROUP,
                "idempotent-order-consumer"
            );
            log.info("Order processed - OrderId: {}, TransactionId: {}", event.orderId(), event.transactionId());
        } catch (Exception e) {
            log.error("Error processing order {} - TransactionId: {}", event.orderId(), event.transactionId(), e);
            throw e;
        }
    }
    
    /**
     * Consumer for PaymentProcessedEvent - Idempotent processing with inconsistency detection
     * This consumer demonstrates:
     * 1. Idempotent processing of payment events
     * 2. Detection of inconsistencies (payment without corresponding order)
     */
    @KafkaListener(
        topics = "payments",
        groupId = PAYMENT_CONSUMER_GROUP,
        containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void consumePaymentProcessedEvent(
        @Payload PaymentProcessedEvent event,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Payment received - PaymentId: {}, OrderId: {}, TransactionId: {}, Amount: {}", 
            event.paymentId(), event.orderId(), event.transactionId(), event.amount());
        
        // Idempotency check - check if payment already processed
        var existingPayment = paymentService.findPaymentByPaymentId(event.paymentId());
        if (existingPayment.isPresent()) {
            log.warn("Payment {} already processed - skipping (idempotency). TransactionId: {}", 
                event.paymentId(), event.transactionId());
            return;
        }
        
        try {
            // Save payment to database
            paymentService.savePayment(event, partition, offset, PAYMENT_CONSUMER_GROUP);
            
            // Check for inconsistency: payment exists but order might not
            // Note: This is a best-effort check. Order event might arrive later (different topic/partition)
            // For accurate inconsistency detection, use the /inconsistencies endpoint which checks with time delays
            if (!paymentService.hasCorrespondingOrder(event.orderId())) {
                log.warn("Payment {} saved but order {} not found yet. TransactionId: {}", 
                    event.paymentId(), event.orderId(), event.transactionId());
                log.warn("Note: Order might arrive later (different topic). Use /inconsistencies endpoint for accurate detection");
            } else {
                log.info("Consistency check passed - Both payment and order exist for order {}. TransactionId: {}", 
                    event.orderId(), event.transactionId());
            }
            
            log.info("Payment processed - PaymentId: {}, TransactionId: {}", event.paymentId(), event.transactionId());
        } catch (Exception e) {
            log.error("Error processing payment {} - TransactionId: {}", event.paymentId(), event.transactionId(), e);
            throw e;
        }
    }
}

