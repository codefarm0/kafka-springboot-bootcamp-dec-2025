package in.codefarm.order.service.as.producer.controller;

import in.codefarm.order.service.as.producer.entity.OrderEntity;
import in.codefarm.order.service.as.producer.event.OrderPlacedEvent;
import in.codefarm.order.service.as.producer.service.OrderEventProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderEventProducerService orderEventProducerService;
    
    public OrderController(OrderEventProducerService orderEventProducerService) {
        this.orderEventProducerService = orderEventProducerService;
    }
    
    // Request DTO
    public record OrderRequest(
        String customerId,
        String productId,
        Integer quantity,
        BigDecimal totalAmount
    ) {
    }
    
    // Helper method to create event from request
    private OrderPlacedEvent createEvent(OrderRequest request) {
        return new OrderPlacedEvent(
            UUID.randomUUID().toString(),
            request.customerId(),
            request.productId(),
            request.quantity(),
            request.totalAmount(),
            LocalDateTime.now(),
                UUID.randomUUID().toString()
        );
    }
    
    // Scenario 1: Fire-and-Forget
    @PostMapping("/fire-and-forget")
    public ResponseEntity<String> placeOrderFireAndForget(@RequestBody OrderRequest request) {
        log.info("=== REST Endpoint: POST /api/orders/fire-and-forget ===");
        
        try {
            var event = createEvent(request);
            var orderEntity = orderEventProducerService.fireAndForget(event);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed (fire-and-forget)! Order ID: " + orderEntity.getOrderId() + 
                      ", DB ID: " + orderEntity.getId());
        } catch (Exception e) {
            log.error("Error in fire-and-forget endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Scenario 2: Synchronous Send
    @PostMapping("/synchronous")
    public ResponseEntity<String> placeOrderSynchronous(@RequestBody OrderRequest request) {
        log.info("=== REST Endpoint: POST /api/orders/synchronous ===");
        
        try {
            var event = createEvent(request);
            var orderEntity = orderEventProducerService.sendSynchronously(event);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed (synchronous)! Order ID: " + orderEntity.getOrderId() + 
                      ", DB ID: " + orderEntity.getId());
        } catch (Exception e) {
            log.error("Error in synchronous endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Scenario 2b: Synchronous Send with Timeout
    @PostMapping("/synchronous-timeout")
    public ResponseEntity<String> placeOrderSynchronousWithTimeout(
        @RequestBody OrderRequest request,
        @RequestParam(defaultValue = "5") long timeoutSeconds
    ) {
        log.info("=== REST Endpoint: POST /api/orders/synchronous-timeout (timeout: {}s) ===", timeoutSeconds);
        
        try {
            var event = createEvent(request);
            var orderEntity = orderEventProducerService.sendSynchronouslyWithTimeout(event, timeoutSeconds);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed (synchronous with timeout)! Order ID: " + orderEntity.getOrderId() + 
                      ", DB ID: " + orderEntity.getId());
        } catch (Exception e) {
            log.error("Error in synchronous-timeout endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Scenario 3: Async with Callback
    @PostMapping("/async-callback")
    public ResponseEntity<String> placeOrderAsyncCallback(@RequestBody OrderRequest request) {
        log.info("=== REST Endpoint: POST /api/orders/async-callback ===");
        
        try {
            var event = createEvent(request);
            var orderEntity = orderEventProducerService.sendWithCallback(event);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed (async with callback)! Order ID: " + orderEntity.getOrderId() + 
                      ", DB ID: " + orderEntity.getId());
        } catch (Exception e) {
            log.error("Error in async-callback endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Scenario 4: Send to Specific Partition
    @PostMapping("/partition/{partition}")
    public ResponseEntity<String> placeOrderToPartition(
        @RequestBody OrderRequest request,
        @PathVariable int partition
    ) {
        log.info("=== REST Endpoint: POST /api/orders/partition/{} ===", partition);
        
        try {
            var event = createEvent(request);
            var orderEntity = orderEventProducerService.sendToPartition(event, partition);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed to partition " + partition + "! Order ID: " + orderEntity.getOrderId() + 
                      ", DB ID: " + orderEntity.getId());
        } catch (Exception e) {
            log.error("Error in partition endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Scenario 5: Send with Headers
    @PostMapping("/with-headers")
    public ResponseEntity<String> placeOrderWithHeaders(@RequestBody OrderRequest request) {
        log.info("=== REST Endpoint: POST /api/orders/with-headers ===");
        
        try {
            var event = createEvent(request);
            var orderEntity = orderEventProducerService.sendWithHeaders(event);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed (with headers)! Order ID: " + orderEntity.getOrderId() + 
                      ", DB ID: " + orderEntity.getId());
        } catch (Exception e) {
            log.error("Error in with-headers endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Scenario 5b: Send with Spring Message
    @PostMapping("/spring-message")
    public ResponseEntity<String> placeOrderSpringMessage(@RequestBody OrderRequest request) {
        log.info("=== REST Endpoint: POST /api/orders/spring-message ===");
        
        try {
            var event = createEvent(request);
            var orderEntity = orderEventProducerService.sendWithSpringMessage(event);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed (Spring Message)! Order ID: " + orderEntity.getOrderId() + 
                      ", DB ID: " + orderEntity.getId());
        } catch (Exception e) {
            log.error("Error in spring-message endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Scenario 6: Send with Timestamp
    @PostMapping("/with-timestamp")
    public ResponseEntity<String> placeOrderWithTimestamp(@RequestBody OrderRequest request) {
        log.info("=== REST Endpoint: POST /api/orders/with-timestamp ===");
        
        try {
            var event = createEvent(request);
            var orderEntity = orderEventProducerService.sendWithTimestamp(event);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Order placed (with timestamp)! Order ID: " + orderEntity.getOrderId() + 
                      ", DB ID: " + orderEntity.getId());
        } catch (Exception e) {
            log.error("Error in with-timestamp endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Scenario 7: Batch Send
    @PostMapping("/batch")
    public ResponseEntity<String> placeOrderBatch(@RequestBody List<OrderRequest> requests) {
        log.info("=== REST Endpoint: POST /api/orders/batch ({} orders) ===", requests.size());
        
        try {
            List<OrderPlacedEvent> events = requests.stream()
                .map(this::createEvent)
                .collect(Collectors.toList());
            
            List<OrderEntity> orderEntities = orderEventProducerService.sendBatch(events);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Batch of " + orderEntities.size() + " orders placed! Order IDs: " + 
                      orderEntities.stream()
                          .map(OrderEntity::getOrderId)
                          .collect(Collectors.joining(", ")));
        } catch (Exception e) {
            log.error("Error in batch endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Scenario 7b: Batch Send Synchronously
    @PostMapping("/batch-synchronous")
    public ResponseEntity<String> placeOrderBatchSynchronous(@RequestBody List<OrderRequest> requests) {
        log.info("=== REST Endpoint: POST /api/orders/batch-synchronous ({} orders) ===", requests.size());
        
        try {
            List<OrderPlacedEvent> events = requests.stream()
                .map(this::createEvent)
                .collect(Collectors.toList());
            
            List<OrderEntity> orderEntities = orderEventProducerService.sendBatchSynchronously(events);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body("Batch (synchronous) of " + orderEntities.size() + " orders placed! Order IDs: " + 
                      orderEntities.stream()
                          .map(OrderEntity::getOrderId)
                          .collect(Collectors.joining(", ")));
        } catch (Exception e) {
            log.error("Error in batch-synchronous endpoint", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is running!");
    }
}

