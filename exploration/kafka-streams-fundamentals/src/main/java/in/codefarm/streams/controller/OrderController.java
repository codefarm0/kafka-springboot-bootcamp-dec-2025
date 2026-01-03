package in.codefarm.streams.controller;

import in.codefarm.streams.event.OrderPlacedEvent;
import in.codefarm.streams.service.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderEventProducer orderEventProducer;
    
    public OrderController(OrderEventProducer orderEventProducer) {
        this.orderEventProducer = orderEventProducer;
    }
    
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        try {
            String orderId = UUID.randomUUID().toString();
            
            // Create OrderPlaced event
            var event = new OrderPlacedEvent(
                orderId,
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.totalAmount(),
                LocalDateTime.now()
            );
            
            orderEventProducer.sendOrderPlacedEvent(event);
            
            log.info("Order placed - OrderId: {}, Amount: {}", orderId, request.totalAmount());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderResponse("success", "Order placed successfully", orderId));
                
        } catch (Exception e) {
            log.error("Error placing order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OrderResponse("error", "Failed to place order: " + e.getMessage(), null));
        }
    }
    
    public record OrderRequest(
        String customerId,
        String productId,
        Integer quantity,
        BigDecimal totalAmount
    ) {
    }
    
    public record OrderResponse(
        String status,
        String message,
        String orderId
    ) {
    }
}

