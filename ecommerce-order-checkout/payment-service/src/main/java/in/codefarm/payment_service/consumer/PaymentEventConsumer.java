package in.codefarm.payment_service.consumer;

import org.springframework.kafka.annotation.BackOff;
import tools.jackson.databind.ObjectMapper;
import in.codefarm.payment_service.event.OrderPlacedEventData;
import in.codefarm.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import in.codefarm.payment_service.exception.PaymentEventPublishingException;

/**
 * Event consumer for Payment Service.
 * Listens to orders topic and processes order.placed events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {
    
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    
    /**
     * Handles order events from the orders topic.
     * Parses JSON string events and filters by event type to process order.placed events.
     * 
     * Retry strategy:
     * - Retries 3 times for PaymentEventPublishingException (transient failures)
     */
    @RetryableTopic(
        attempts = "1",  // Retry 3 times (total 4 attempts: 1 initial + 3 retries)
        backOff = @BackOff(delay = 1000, multiplier = 2.0),  // 1s, 2s, 4s delays
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        include = {PaymentEventPublishingException.class}  // Retry on transient failures
    )
    @KafkaListener(
        topics = "orders",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String eventId = null;
        String orderId = null;
        OrderPlacedEventData orderData = null;
        
        try {
            // Parse event wrapper - use a more flexible approach
            // First parse as a generic map to handle eventTime string properly
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> eventMap = objectMapper.readValue(
                eventJson,
                java.util.Map.class
            );
            
            eventType = (String) eventMap.get("eventType");
            eventId = (String) eventMap.get("eventId");
            
            // Parse the data field as OrderPlacedEventData
            Object dataObj = eventMap.get("data");
            if (dataObj != null) {
                orderData = objectMapper.convertValue(dataObj, OrderPlacedEventData.class);
                if (orderData != null) {
                    orderId = orderData.getOrderId();
                }
            }
            
            log.info("Received order event: type={}, id={}, orderId={}, partition={}, offset={}",
                eventType,
                eventId,
                orderId,
                consumerRecord.partition(),
                consumerRecord.offset());
            
            // Only process order.placed events
            if (!"com.ecommerce.order.placed".equals(eventType)) {
                log.debug("Ignoring event type: {}, expected: com.ecommerce.order.placed", eventType);
                acknowledgment.acknowledge();
                return;
            }
            
            if (orderId == null || orderData == null) {
                log.warn("OrderPlaced event missing orderId or data, skipping: eventId={}", eventId);
                acknowledgment.acknowledge();
                return;
            }
            
            log.info("Processing payment for order: orderId={}, customerId={}, amount={}", 
                orderId, orderData.getCustomerId(), orderData.getTotalAmount());
            
            // Process payment (with idempotency check using event ID)
            // Pass full order data so it can be forwarded to downstream services
            paymentService.processPayment(
                eventId,  // Use event ID for idempotency
                orderId,
                orderData.getCustomerId(),
                orderData.getTotalAmount(),
                orderData  // Pass full order data
            );
            
            log.info("Payment processing completed: orderId={}", orderId);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing order event: type={}, id={}, orderId={}, eventJson={}",
                eventType, eventId, orderId, eventJson, e);
            // Log the root cause
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage(), e.getCause());
            }
            // Don't acknowledge - message will be retried
            throw new RuntimeException("Failed to process order event: " + e.getMessage(), e);
        }
    }
}

