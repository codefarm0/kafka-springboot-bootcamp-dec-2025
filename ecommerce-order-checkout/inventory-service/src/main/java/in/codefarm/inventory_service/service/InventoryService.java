package in.codefarm.inventory_service.service;

import in.codefarm.inventory_service.exception.OrderItemsMissingException;
import in.codefarm.inventory_service.exception.ProductNotFoundException;
import in.codefarm.inventory_service.exception.InsufficientInventoryException;
import in.codefarm.inventory_service.exception.InventoryEventPublishingException;
import tools.jackson.databind.ObjectMapper;
import in.codefarm.inventory_service.entity.IdempotencyKey;
import in.codefarm.inventory_service.entity.Product;
import in.codefarm.inventory_service.entity.Reservation;
import in.codefarm.inventory_service.event.EventService;
import in.codefarm.inventory_service.event.InventoryReservationFailedEventData;
import in.codefarm.inventory_service.event.InventoryReservedEventData;
import in.codefarm.inventory_service.event.PaymentProcessedEventData;
import in.codefarm.inventory_service.repository.IdempotencyKeyRepository;
import in.codefarm.inventory_service.repository.ProductRepository;
import in.codefarm.inventory_service.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Inventory Service implementation with idempotency.
 * Reserves inventory for orders and publishes reservation events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    
    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final EventService eventService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final String INVENTORY_TOPIC = "inventory";
    
    /**
     * Reserves inventory for an order after payment is processed.
     * Implements idempotency to ensure exactly-once processing.
     * 
     * @param eventId CloudEvents ID for idempotency check
     * @param orderId Order ID
     * @param orderItems Order items from payment event
     * @param shippingAddress Shipping address for downstream service
     * @param customerId Customer ID for downstream service
     * @return List of reservations created
     */
    @Transactional
    public List<Reservation> reserveInventory(String eventId, String orderId, 
                                             List<PaymentProcessedEventData.OrderItemData> orderItems,
                                             String shippingAddress, String customerId) {
        log.info("Reserving inventory: eventId={}, orderId={}, items={}", eventId, orderId, 
            orderItems != null ? orderItems.size() : 0);
        
        // Idempotency check: if event already processed, return existing reservations
        if (idempotencyKeyRepository.existsByEventId(eventId)) {
            log.info("Event already processed (idempotency): eventId={}, orderId={}", eventId, orderId);
            List<Reservation> existingReservations = reservationRepository.findByOrderId(orderId);
            log.info("Returning existing reservations: orderId={}, count={}", orderId, existingReservations.size());
            return existingReservations;
        }
        
        // Validate order items - business logic failure, send directly to DLT
        if (orderItems == null || orderItems.isEmpty()) {
            String errorMsg = "Order items not provided: orderId=" + orderId;
            log.error(errorMsg);
            // Don't publish event here - transaction will rollback
            // Event will be published in consumer after exception is caught
            
            // Save idempotency key even for failures
            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .eventId(eventId)
                .orderId(orderId)
                .eventType("com.ecommerce.payment.processed")
                .build();
            idempotencyKeyRepository.save(idempotencyKey);
            
            // Business logic failure: order items missing, send directly to DLT
            throw new OrderItemsMissingException(errorMsg);
        }
        
        List<Reservation> reservations = new ArrayList<>();
        List<InventoryReservedEventData.ReservedItemData> reservedItems = new ArrayList<>();
        
        // Reserve inventory for each order item
        for (PaymentProcessedEventData.OrderItemData orderItem : orderItems) {
            String productId = orderItem.getProductId();
            int requestedQuantity = orderItem.getQuantity();
            
            log.info("Reserving inventory: productId={}, quantity={}, orderId={}", 
                productId, requestedQuantity, orderId);
            
            // Get product with pessimistic lock to prevent race conditions
            Product product = productRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> {
                    log.error("Product not found: productId={}", productId);
                    // Don't publish event here - transaction will rollback
                    // Event will be published after transaction commits in catch block
                    // Business logic failure: product doesn't exist, send directly to DLT
                    return new ProductNotFoundException(productId);
                });
            
            // Check if sufficient inventory is available
            if (!product.hasAvailableQuantity(requestedQuantity)) {
                log.warn("Insufficient inventory: productId={}, requested={}, available={}",
                    productId, requestedQuantity, product.getAvailableQuantity());
                
                // Mark all previous reservations as failed
                reservations.forEach(r -> {
                    r.setStatus(Reservation.ReservationStatus.FAILED);
                    r.setFailureReason("Insufficient inventory for product: " + productId);
                });
                reservationRepository.saveAll(reservations);
                
                // Don't publish event here - transaction will rollback
                // Event will be published in consumer after exception is caught
                
                // Save idempotency key
                IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                    .eventId(eventId)
                    .orderId(orderId)
                    .eventType("com.ecommerce.payment.processed")
                    .build();
                idempotencyKeyRepository.save(idempotencyKey);
                
                // Business logic failure: insufficient inventory, send directly to DLT
                throw new InsufficientInventoryException(productId, requestedQuantity, product.getAvailableQuantity());
            }
            
            // Create reservation
            String reservationId = "reservation-" + UUID.randomUUID().toString();
            Reservation reservation = Reservation.builder()
                .reservationId(reservationId)
                .orderId(orderId)
                .product(product)
                .quantity(requestedQuantity)
                .status(Reservation.ReservationStatus.RESERVED)
                .build();
            
            reservation = reservationRepository.save(reservation);
            reservations.add(reservation);
            
            // Update product reserved quantity
            product.setReservedQuantity(product.getReservedQuantity() + requestedQuantity);
            productRepository.save(product);
            
            log.info("Inventory reserved: reservationId={}, productId={}, quantity={}, orderId={}", 
                reservationId, productId, requestedQuantity, orderId);
            
            // Add to reserved items for event
            reservedItems.add(InventoryReservedEventData.ReservedItemData.builder()
                .productId(productId)
                .productName(orderItem.getProductName())
                .quantity(requestedQuantity)
                .reservationId(reservationId)
                .build());
        }
        
        // Save idempotency key
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
            .eventId(eventId)
            .orderId(orderId)
            .eventType("com.ecommerce.payment.processed")
            .build();
        idempotencyKeyRepository.save(idempotencyKey);
        
        // Publish InventoryReservedEvent (with shipping address and customer ID for shipping service)
        publishInventoryReservedEvent(orderId, reservedItems, shippingAddress, customerId);
        
        log.info("Inventory reservation completed: orderId={}, reservations={}", 
            orderId, reservations.size());
        
        return reservations;
    }
    
    /**
     * Publishes InventoryReservedEvent to inventory topic.
     * Includes shipping address and customer ID for shipping service.
     */
    private void publishInventoryReservedEvent(String orderId, 
                                               List<InventoryReservedEventData.ReservedItemData> reservedItems,
                                               String shippingAddress, String customerId) {
        try {
            InventoryReservedEventData eventData = InventoryReservedEventData.builder()
                .orderId(orderId)
                .reservedItems(reservedItems)
                .shippingAddress(shippingAddress)
                .customerId(customerId)
                .build();
            
            String eventJson = eventService.createEvent(
                "com.ecommerce.inventory.reserved",
                null, // Auto-generate event ID
                eventData
            );
            
            // Use order ID as Kafka key for partition ordering
            kafkaTemplate.send(INVENTORY_TOPIC, orderId, eventJson);
            
            log.info("Published InventoryReservedEvent: orderId={}, items={}", 
                orderId, reservedItems.size());
                
        } catch (Exception e) {
            log.error("Failed to publish InventoryReservedEvent: orderId={}", orderId, e);
            // Transient failure: publishing failures are often transient (network issues)
            throw new InventoryEventPublishingException("Failed to publish inventory reserved event", e);
        }
    }
    
    /**
     * Publishes InventoryReservationFailedEvent to inventory topic.
     * Made public so it can be called from consumer after transaction rollback.
     */
    @Transactional
    public void publishInventoryReservationFailedEvent(String orderId, String failureReason, String productId) {
        try {
            InventoryReservationFailedEventData eventData = InventoryReservationFailedEventData.builder()
                .orderId(orderId)
                .failureReason(failureReason)
                .productId(productId)
                .build();
            
            String eventJson = eventService.createEvent(
                "com.ecommerce.inventory.reservation.failed",
                null, // Auto-generate event ID
                eventData
            );
            
            // Use order ID as Kafka key for partition ordering
            kafkaTemplate.send(INVENTORY_TOPIC, orderId, eventJson);
            
            log.info("Published InventoryReservationFailedEvent: orderId={}, reason={}", 
                orderId, failureReason);
                
        } catch (Exception e) {
            log.error("Failed to publish InventoryReservationFailedEvent: orderId={}", orderId, e);
            // Transient failure: publishing failures are often transient (network issues)
            throw new InventoryEventPublishingException("Failed to publish inventory reservation failed event", e);
        }
    }
}

