package in.codefarm.saga.order.controller;

import in.codefarm.saga.order.entity.OrderEntity;
import in.codefarm.saga.order.service.OrderService;
import in.codefarm.saga.order.service.OrderEventProducer;
import in.codefarm.saga.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    private final OrderEventProducer orderEventProducer;
    
    public OrderController(OrderService orderService, OrderEventProducer orderEventProducer) {
        this.orderService = orderService;
        this.orderEventProducer = orderEventProducer;
    }
    
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        try {
            String orderId = UUID.randomUUID().toString();
            String transactionId = UUID.randomUUID().toString();
            
            // Create order in database
            var order = orderService.createOrder(
                orderId,
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.totalAmount(),
                transactionId
            );
            
            // Publish OrderPlaced event
            var event = new OrderPlacedEvent(
                orderId,
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.totalAmount(),
                LocalDateTime.now()
            );
            
            orderEventProducer.sendOrderPlacedEvent(event, transactionId);
            
            log.info("Order placed - OrderId: {}, TransactionId: {}", orderId, transactionId);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new OrderResponse("success", "Order placed successfully", orderId, transactionId));
                
        } catch (Exception e) {
            log.error("Error placing order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OrderResponse("error", "Failed to place order: " + e.getMessage(), null, null));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<OrderEntity>> getAllOrders() {
        return ResponseEntity.ok(orderService.findAll());
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderEntity> getOrder(@PathVariable String orderId) {
        return orderService.findByOrderId(orderId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
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
        String orderId,
        String transactionId
    ) {
    }
}

