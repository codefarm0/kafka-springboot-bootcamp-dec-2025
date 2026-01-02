package in.codefarm.saga.payment.consumer;

import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.OrderPlacedEvent;
import in.codefarm.saga.event.InventoryReservedEvent;
import in.codefarm.saga.event.PaymentProcessedEvent;
import in.codefarm.saga.event.PaymentRefundedEvent;
import in.codefarm.saga.payment.service.PaymentService;
import in.codefarm.saga.payment.service.PaymentEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class PaymentServiceConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceConsumer.class);
    
    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;
    private final JsonMapper jsonMapper;
    
    public PaymentServiceConsumer(
        PaymentService paymentService,
        PaymentEventProducer paymentEventProducer,
        @Qualifier("consumerJsonMapper") JsonMapper jsonMapper
    ) {
        this.paymentService = paymentService;
        this.paymentEventProducer = paymentEventProducer;
        this.jsonMapper = jsonMapper;
    }
    
    @KafkaListener(
        topics = "orders",
        groupId = "payment-service-group",
        containerFactory = "eventWrapperKafkaListenerContainerFactory"
    )
    public void handleOrderPlaced(@Payload EventWrapper<?> wrapper) {
        try {
            String eventType = wrapper.metadata().eventType();
            
            // Only process OrderPlaced events, ignore OrderCancelled events
            if (!"OrderPlaced".equals(eventType)) {
                log.debug("Payment Service: Ignoring event type: {} - only processing OrderPlaced events", eventType);
                return;
            }
            
            // Extract payload based on event type
            OrderPlacedEvent event = jsonMapper.convertValue(wrapper.payload(), OrderPlacedEvent.class);
            String transactionId = wrapper.metadata().transactionId();
            
            log.info("Payment Service: Received OrderPlaced event - OrderId: {}, TransactionId: {}", 
                event.orderId(), transactionId);
            
            // Idempotency check
            var existingPayment = paymentService.findByOrderId(event.orderId());
            if (existingPayment.isPresent()) {
                log.warn("Payment already processed for order: {} - skipping (idempotency)", event.orderId());
                return;
            }
            
            boolean paymentSuccess = processPayment(event);
            String paymentId = UUID.randomUUID().toString();
            
            // Save payment to database
            paymentService.processPayment(
                paymentId,
                event.orderId(),
                event.customerId(),
                event.totalAmount(),
                paymentSuccess ? "SUCCESS" : "FAILED",
                transactionId
            );
            
            // Publish result
            var paymentEvent = new PaymentProcessedEvent(
                paymentId,
                event.orderId(),
                event.customerId(),
                event.totalAmount(),
                paymentSuccess ? "SUCCESS" : "FAILED",
                LocalDateTime.now()
            );
            
            paymentEventProducer.sendPaymentProcessedEvent(paymentEvent, transactionId);
            
            log.info("Payment Service: Payment processed - OrderId: {}, Status: {}, TransactionId: {}", 
                event.orderId(), paymentEvent.status(), transactionId);
            
        } catch (Exception e) {
            log.error("Payment Service: Error processing payment", e);
            
            // Try to extract orderId from wrapper for failure event
            try {
                OrderPlacedEvent event = jsonMapper.convertValue(wrapper.payload(), OrderPlacedEvent.class);
                String transactionId = wrapper.metadata().transactionId();
                
                var failureEvent = new PaymentProcessedEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    event.customerId(),
                    event.totalAmount(),
                    "FAILED",
                    LocalDateTime.now()
                );
                
                paymentEventProducer.sendPaymentProcessedEvent(failureEvent, transactionId);
            } catch (Exception ex) {
                log.error("Failed to send failure event", ex);
            }
        }
    }
    
    @KafkaListener(
        topics = "inventory",
        groupId = "payment-service-compensation-group",
        containerFactory = "eventWrapperKafkaListenerContainerFactory"
    )
    public void handleInventoryUnavailable(@Payload EventWrapper<?> wrapper) {
        try {
            InventoryReservedEvent event = jsonMapper.convertValue(wrapper.payload(), InventoryReservedEvent.class);
            String transactionId = wrapper.metadata().transactionId();
            
            if ("UNAVAILABLE".equals(event.status())) {
                log.info("Payment Service: Inventory unavailable - initiating refund - OrderId: {}, TransactionId: {}", 
                    event.orderId(), transactionId);
                
                // Refund payment
                paymentService.refundPayment(event.orderId());
                
                var payment = paymentService.findByOrderId(event.orderId());
                if (payment.isPresent()) {
                    // Publish refund event
                    var refundEvent = new PaymentRefundedEvent(
                        payment.get().getPaymentId(),
                        event.orderId(),
                        payment.get().getAmount(),
                        LocalDateTime.now()
                    );
                    
                    paymentEventProducer.sendPaymentRefundedEvent(refundEvent, transactionId);
                }
            }
        } catch (Exception e) {
            log.error("Payment Service: Error handling inventory unavailable", e);
        }
    }
    
    private boolean processPayment(OrderPlacedEvent event) {
        // Simulate payment processing logic
        // In real system: call payment gateway, validate card, etc.
        
        // Simulate: Fail if amount > 1000
        if (event.totalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            log.warn("Payment failed: Amount {} exceeds limit", event.totalAmount());
            return false;
        }
        
        log.info("Payment successful for amount: {}", event.totalAmount());
        return true;
    }
}

