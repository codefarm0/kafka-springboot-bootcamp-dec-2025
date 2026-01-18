package in.codefarm.order_service.controller;

import in.codefarm.order_service.dto.CreateOrderRequest;
import in.codefarm.order_service.dto.OrderResponse;
import in.codefarm.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received create order request for customer: {}", request.getCustomerId());
        
        OrderResponse order = orderService.createOrder(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        log.info("Received get order request: {}", orderId);
        
        OrderResponse order = orderService.getOrder(orderId);
        
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<java.util.List<OrderResponse>> getOrdersByCustomer(
            @PathVariable String customerId) {
        log.info("Received get orders request for customer: {}", customerId);
        
        java.util.List<OrderResponse> orders = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(orders);
    }
}

