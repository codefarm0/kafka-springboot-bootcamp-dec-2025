package in.codefarm.payment_service.service;

import in.codefarm.payment_service.exception.PaymentEventPublishingException;
import tools.jackson.databind.ObjectMapper;
import in.codefarm.payment_service.entity.IdempotencyKey;
import in.codefarm.payment_service.entity.Payment;
import in.codefarm.payment_service.event.EventService;
import in.codefarm.payment_service.event.OrderPlacedEventData;
import in.codefarm.payment_service.event.PaymentFailedEventData;
import in.codefarm.payment_service.event.PaymentProcessedEventData;
import in.codefarm.payment_service.repository.IdempotencyKeyRepository;
import in.codefarm.payment_service.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Payment Service implementation with idempotency.
 * Processes payments for orders and publishes payment events.
 */
@Service
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final EventService eventService;
    @Qualifier("transactionalKafkaTemplate")
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final String PAYMENTS_TOPIC = "payments";
    private static final String DEFAULT_PAYMENT_METHOD = "CREDIT_CARD";
    
    public PaymentService(
            PaymentRepository paymentRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            PaymentGatewayClient paymentGatewayClient,
            EventService eventService,
            @Qualifier("transactionalKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.eventService = eventService;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Processes payment for an order placed event.
     * Implements idempotency to ensure exactly-once processing.
     * 
     * @param eventId Event ID for idempotency check
     * @param orderId Order ID
     * @param customerId Customer ID
     * @param amount Payment amount
     * @param orderData Full order data (for passing to downstream services)
     * @return Payment entity
     */
    @Transactional
    public Payment processPayment(String eventId, String orderId, String customerId, BigDecimal amount, OrderPlacedEventData orderData) {
        log.info("Processing payment: eventId={}, orderId={}, customerId={}, amount={}", 
            eventId, orderId, customerId, amount);
        
        // Idempotency check: if event already processed, return existing payment
        if (idempotencyKeyRepository.existsByEventId(eventId)) {
            log.info("Event already processed (idempotency): eventId={}, orderId={}", eventId, orderId);
            IdempotencyKey existingKey = idempotencyKeyRepository.findByEventId(eventId)
                .orElseThrow(() -> new RuntimeException("Idempotency key not found: " + eventId));
            
            Payment existingPayment = paymentRepository.findByOrderId(orderId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
            
            log.info("Returning existing payment: paymentId={}, orderId={}", 
                existingPayment.getPaymentId(), orderId);
            return existingPayment;
        }
        
        // Generate payment ID
        String paymentId = "payment-" + UUID.randomUUID().toString();
        
        // Create payment record with PENDING status
        Payment payment = Payment.builder()
            .paymentId(paymentId)
            .orderId(orderId)
            .customerId(customerId)
            .amount(amount)
            .status(Payment.PaymentStatus.PROCESSING)
            .paymentMethod(DEFAULT_PAYMENT_METHOD)
            .build();
        
        payment = paymentRepository.save(payment);
        log.info("Payment record created: paymentId={}, orderId={}", paymentId, orderId);
        
        // Process payment through gateway
        PaymentGatewayClient.PaymentResult result = paymentGatewayClient.processPayment(
            orderId, amount, DEFAULT_PAYMENT_METHOD);
        
        // Update payment status based on gateway result
        if (result.isSuccess()) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setTransactionId(result.getTransactionId());
            payment = paymentRepository.save(payment);
            
            log.info("Payment completed: paymentId={}, orderId={}, transactionId={}", 
                paymentId, orderId, result.getTransactionId());
            
            // Save idempotency key
            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .eventId(eventId)
                .orderId(orderId)
                .eventType("com.ecommerce.order.placed")
                .build();
            idempotencyKeyRepository.save(idempotencyKey);
            
            // Publish PaymentProcessedEvent (with full order data for downstream services)
            String paymentEventId = publishPaymentProcessedEvent(payment, orderData);
            
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(result.getFailureReason());
            payment = paymentRepository.save(payment);
            
            log.warn("Payment failed: paymentId={}, orderId={}, reason={}", 
                paymentId, orderId, result.getFailureReason());
            
            // Save idempotency key even for failures (to prevent reprocessing)
            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .eventId(eventId)
                .orderId(orderId)
                .eventType("com.ecommerce.order.placed")
                .build();
            idempotencyKeyRepository.save(idempotencyKey);
            
            // Publish PaymentFailedEvent
            String failureEventId = publishPaymentFailedEvent(payment, result.getFailureReason());
        }
        
        return payment;
    }
    
    /**
     * Publishes PaymentProcessedEvent to payments topic.
     * Includes order items and shipping address for downstream services.
     * @return Event ID
     */
    private String publishPaymentProcessedEvent(Payment payment, OrderPlacedEventData orderData) {
        try {
            // Convert OrderPlacedEventData.OrderItemData to PaymentProcessedEventData.OrderItemData
            List<PaymentProcessedEventData.OrderItemData> items = orderData.getItems().stream()
                .map(item -> PaymentProcessedEventData.OrderItemData.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getTotalPrice())
                    .build())
                .toList();
            
            PaymentProcessedEventData eventData = PaymentProcessedEventData.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .paymentMethod(payment.getPaymentMethod())
                .items(items)
                .shippingAddress(orderData.getShippingAddress())
                .build();
            
            String eventJson = eventService.createEvent(
                "com.ecommerce.payment.processed",
                null, // Auto-generate event ID
                eventData
            );
            
            // Use order ID as Kafka key for partition ordering
            kafkaTemplate.send(PAYMENTS_TOPIC, payment.getOrderId(), eventJson);
            
            // Extract event ID from JSON for logging (optional)
            log.info("Published PaymentProcessedEvent: paymentId={}, orderId={}", 
                payment.getPaymentId(), payment.getOrderId());
            
            return null; // Event ID is in the JSON, not needed here
                
        } catch (Exception e) {
            log.error("Failed to publish PaymentProcessedEvent: paymentId={}, orderId={}", 
                payment.getPaymentId(), payment.getOrderId(), e);
            // Transient failure: publishing failures are often transient (network issues)
            throw new PaymentEventPublishingException("Failed to publish payment processed event", e);
        }
    }
    
    /**
     * Publishes PaymentFailedEvent to payments topic.
     * @return Event ID
     */
    private String publishPaymentFailedEvent(Payment payment, String failureReason) {
        try {
            PaymentFailedEventData eventData = PaymentFailedEventData.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .failureReason(failureReason)
                .build();
            
            String eventJson = eventService.createEvent(
                "com.ecommerce.payment.failed",
                null, // Auto-generate event ID
                eventData
            );
            
            // Use order ID as Kafka key for partition ordering
            kafkaTemplate.send(PAYMENTS_TOPIC, payment.getOrderId(), eventJson);
            
            log.info("Published PaymentFailedEvent: paymentId={}, orderId={}", 
                payment.getPaymentId(), payment.getOrderId());
            
            return null; // Event ID is in the JSON, not needed here
                
        } catch (Exception e) {
            log.error("Failed to publish PaymentFailedEvent: paymentId={}, orderId={}", 
                payment.getPaymentId(), payment.getOrderId(), e);
            // Transient failure: publishing failures are often transient (network issues)
            throw new PaymentEventPublishingException("Failed to publish payment failed event", e);
        }
    }
}

