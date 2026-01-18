package in.codefarm.shipping_service.service;

import in.codefarm.shipping_service.entity.IdempotencyKey;
import in.codefarm.shipping_service.entity.Shipment;
import in.codefarm.shipping_service.event.EventService;
import in.codefarm.shipping_service.event.ShippingFailedEventData;
import in.codefarm.shipping_service.event.ShippingLabelCreatedEventData;
import in.codefarm.shipping_service.exception.ShippingAddressMissingException;
import in.codefarm.shipping_service.exception.ShippingEventPublishingException;
import in.codefarm.shipping_service.repository.IdempotencyKeyRepository;
import in.codefarm.shipping_service.repository.ShipmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Shipping Service implementation with idempotency.
 * Creates shipping labels for orders and publishes shipping events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {
    
    private final ShipmentRepository shipmentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ShippingApiClient shippingApiClient;
    private final EventService eventService;
    @Qualifier("transactionalKafkaTemplate")
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final String SHIPPING_TOPIC = "shipping";
    
    /**
     * Creates shipping label for an order after inventory is reserved.
     * Implements idempotency to ensure exactly-once processing.
     * 
     * @param eventId CloudEvents ID for idempotency check
     * @param orderId Order ID
     * @param shippingAddress Shipping address from inventory event
     * @param customerId Customer ID from inventory event
     * @return Shipment entity
     */
    @Transactional
    public Shipment createShippingLabel(String eventId, String orderId, String shippingAddress, String customerId) {
        log.info("Creating shipping label: eventId={}, orderId={}, shippingAddress={}, customerId={}", 
            eventId, orderId, shippingAddress, customerId);
        
        // Idempotency check: if event already processed, return existing shipment
        if (idempotencyKeyRepository.existsByEventId(eventId)) {
            log.info("Event already processed (idempotency): eventId={}, orderId={}", eventId, orderId);
            Shipment existingShipment = shipmentRepository.findByOrderId(orderId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Shipment not found for order: " + orderId));
            
            log.info("Returning existing shipment: shipmentId={}, orderId={}", 
                existingShipment.getShipmentId(), orderId);
            return existingShipment;
        }
        
        // Validate shipping address
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            String errorMsg = "Shipping address not provided: orderId=" + orderId;
            log.error(errorMsg);
            publishShippingFailedEvent(orderId, errorMsg);
            
            // Save idempotency key even for failures
            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .eventId(eventId)
                .orderId(orderId)
                .eventType("com.ecommerce.inventory.reserved")
                .build();
            idempotencyKeyRepository.save(idempotencyKey);
            
            // Business logic failure: shipping address missing, send directly to DLT
            throw new ShippingAddressMissingException(errorMsg);
        }
        
        // Generate shipment ID
        String shipmentId = "shipment-" + UUID.randomUUID().toString();
        
        // Create shipment record with PENDING status
        Shipment shipment = Shipment.builder()
            .shipmentId(shipmentId)
            .orderId(orderId)
            .customerId(customerId)
            .shippingAddress(shippingAddress)
            .status(Shipment.ShipmentStatus.PENDING)
            .build();
        
        shipment = shipmentRepository.save(shipment);
        log.info("Shipment record created: shipmentId={}, orderId={}", shipmentId, orderId);
        
        // Create shipping label through shipping API
        ShippingApiClient.ShippingResult result = shippingApiClient.createShippingLabel(
            orderId, shippingAddress);
        
        // Update shipment status based on API result
        if (result.isSuccess()) {
            shipment.setStatus(Shipment.ShipmentStatus.LABEL_CREATED);
            shipment.setTrackingNumber(result.getTrackingNumber());
            shipment.setCarrier(result.getCarrier());
            shipment = shipmentRepository.save(shipment);
            
            log.info("Shipping label created: shipmentId={}, orderId={}, trackingNumber={}, carrier={}", 
                shipmentId, orderId, result.getTrackingNumber(), result.getCarrier());
            
            // Save idempotency key
            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .eventId(eventId)
                .orderId(orderId)
                .eventType("com.ecommerce.inventory.reserved")
                .build();
            idempotencyKeyRepository.save(idempotencyKey);
            
            // Publish ShippingLabelCreatedEvent (also known as shipping.arranged)
            publishShippingLabelCreatedEvent(shipment);
            
        } else {
            shipment.setStatus(Shipment.ShipmentStatus.FAILED);
            shipment.setFailureReason(result.getFailureReason());
            shipment = shipmentRepository.save(shipment);
            
            log.warn("Shipping label creation failed: shipmentId={}, orderId={}, reason={}", 
                shipmentId, orderId, result.getFailureReason());
            
            // Save idempotency key even for failures
            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .eventId(eventId)
                .orderId(orderId)
                .eventType("com.ecommerce.inventory.reserved")
                .build();
            idempotencyKeyRepository.save(idempotencyKey);
            
            // Publish ShippingFailedEvent
            publishShippingFailedEvent(orderId, result.getFailureReason());
        }
        
        return shipment;
    }
    
    /**
     * Publishes ShippingLabelCreatedEvent to shipping topic.
     * Also publishes as shipping.arranged for compatibility.
     */
    private void publishShippingLabelCreatedEvent(Shipment shipment) {
        try {
            ShippingLabelCreatedEventData eventData = ShippingLabelCreatedEventData.builder()
                .shipmentId(shipment.getShipmentId())
                .orderId(shipment.getOrderId())
                .customerId(shipment.getCustomerId())
                .trackingNumber(shipment.getTrackingNumber())
                .carrier(shipment.getCarrier())
                .shippingAddress(shipment.getShippingAddress())
                .build();
            
            // Publish as shipping.label.created (primary event type)
            String eventJson = eventService.createEvent(
                "com.ecommerce.shipping.label.created",
                null, // Auto-generate event ID
                eventData
            );
            
            // Use order ID as Kafka key for partition ordering
            kafkaTemplate.send(SHIPPING_TOPIC, shipment.getOrderId(), eventJson);
            
            log.info("Published ShippingLabelCreatedEvent: shipmentId={}, orderId={}", 
                shipment.getShipmentId(), shipment.getOrderId());
                
        } catch (Exception e) {
            log.error("Failed to publish ShippingLabelCreatedEvent: shipmentId={}, orderId={}", 
                shipment.getShipmentId(), shipment.getOrderId(), e);
            // Transient failure: publishing failures are often transient (network issues)
            throw new ShippingEventPublishingException("Failed to publish shipping label created event", e);
        }
    }
    
    /**
     * Publishes ShippingFailedEvent to shipping topic.
     */
    private void publishShippingFailedEvent(String orderId, String failureReason) {
        try {
            ShippingFailedEventData eventData = ShippingFailedEventData.builder()
                .orderId(orderId)
                .failureReason(failureReason)
                .build();
            
            String eventJson = eventService.createEvent(
                "com.ecommerce.shipping.failed",
                null, // Auto-generate event ID
                eventData
            );
            
            // Use order ID as Kafka key for partition ordering
            kafkaTemplate.send(SHIPPING_TOPIC, orderId, eventJson);
            
            log.info("Published ShippingFailedEvent: orderId={}, reason={}", 
                orderId, failureReason);
                
        } catch (Exception e) {
            log.error("Failed to publish ShippingFailedEvent: orderId={}", orderId, e);
            // Transient failure: publishing failures are often transient (network issues)
            throw new ShippingEventPublishingException("Failed to publish shipping failed event", e);
        }
    }
}

