package in.codefarm.schema.avro.controller;

import in.codefarm.schema.avro.event.OrderPlacedEvent;
import in.codefarm.schema.avro.producer.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST Controller for Order Management
 * 
 * DEMO NOTES:
 * - Creates orders and publishes Avro-serialized events
 * - Demonstrates Avro producer usage
 * - Events are automatically registered with Schema Registry
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderEventProducer orderEventProducer;
    
    public OrderController(OrderEventProducer orderEventProducer) {
        this.orderEventProducer = orderEventProducer;
    }
    
    /**
     * Place an order and publish Avro event
     * 
     * DEMO NOTES:
     * - Creates OrderPlacedEvent using Avro-generated class
     * - Event is serialized to Avro binary format
     * - Schema is registered with Schema Registry automatically
     * 
     * @param request Order request
     * @return Order response
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        try {
            String orderId = UUID.randomUUID().toString();
            
            // Create Avro event using generated class
            // Note: discount field is optional (default null), so we don't need to set it
            OrderPlacedEvent event = OrderPlacedEvent.newBuilder()
                .setOrderId(orderId)
                .setCustomerId(request.customerId())
                .setProductId(request.productId())
                .setQuantity(request.quantity())
                .setTotalAmount(request.totalAmount())
                .setOrderDate(LocalDateTime.now().toString())
                .setDiscount(null)  // Optional field, defaults to null
                .build();
            
            // Send Avro-serialized event
            orderEventProducer.sendOrderPlacedEvent(event);
            
            log.info("Order placed (Avro) - OrderId: {}, Amount: {}", orderId, request.totalAmount());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderResponse("success", "Order placed successfully", orderId));
                
        } catch (Exception e) {
            log.error("Error placing order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OrderResponse("error", "Failed to place order: " + e.getMessage(), null));
        }
    }
    
    /**
     * Place an order with discount (demonstrates schema evolution)
     * 
     * DEMO NOTES:
     * - Uses the evolved OrderPlacedEvent schema (now includes discount field)
     * - Demonstrates backward compatibility
     * - Old consumers can still read new events (they ignore discount field)
     * - The discount field is optional with default null, making it backward compatible
     */
    @PostMapping("/with-discount")
    public ResponseEntity<OrderResponse> placeOrderWithDiscount(@RequestBody OrderRequestWithDiscount request) {
        try {
            String orderId = UUID.randomUUID().toString();
            
            // Use the evolved OrderPlacedEvent schema (now includes discount field)
            // The discount field is optional with default null, making it backward compatible
            OrderPlacedEvent event = OrderPlacedEvent.newBuilder()
                .setOrderId(orderId)
                .setCustomerId(request.customerId())
                .setProductId(request.productId())
                .setQuantity(request.quantity())
                .setTotalAmount(request.totalAmount())
                .setOrderDate(LocalDateTime.now().toString())
                .setDiscount(request.discount)  // Set discount value
                .build();

            orderEventProducer.sendOrderPlacedEvent(event);
            
            log.info("Order placed with discount (Avro) - OrderId: {}, Amount: {}, Discount: {}", 
                orderId, request.totalAmount(), request.discount());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderResponse("success", "Order placed successfully", orderId));
                
        } catch (Exception e) {
            log.error("Error placing order with discount", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OrderResponse("error", "Failed to place order: " + e.getMessage(), null));
        }
    }
    
    public record OrderRequest(
        String customerId,
        String productId,
        Integer quantity,
        Double totalAmount
    ) {
    }
    
    public record OrderRequestWithDiscount(
        String customerId,
        String productId,
        Integer quantity,
        Double totalAmount,
        Double discount
    ) {
    }
    
    public record OrderResponse(
        String status,
        String message,
        String orderId
    ) {
    }
}

