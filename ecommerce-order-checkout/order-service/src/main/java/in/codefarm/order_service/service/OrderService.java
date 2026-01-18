package in.codefarm.order_service.service;

import in.codefarm.order_service.dto.CreateOrderRequest;
import in.codefarm.order_service.dto.OrderResponse;
import in.codefarm.order_service.entity.Order;
import in.codefarm.order_service.entity.OrderItem;
import in.codefarm.order_service.entity.OutboxEvent;
import in.codefarm.order_service.event.OrderPlacedEventData;
import in.codefarm.order_service.repository.OrderRepository;
import in.codefarm.order_service.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Order Service implementation with Outbox Pattern.
 * Orders and outbox events are written in the same transaction.
 * Debezium CDC captures outbox events and publishes them to Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new order and writes an outbox event in the same transaction.
     * The outbox event will be captured by Debezium CDC and published to Kafka.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        
        // Generate order ID
        String orderId = "order-" + UUID.randomUUID().toString();
        
        // Calculate total amount
        BigDecimal totalAmount = request.getItems().stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Create order entity
        Order order = Order.builder()
            .id(orderId)
            .customerId(request.getCustomerId())
            .totalAmount(totalAmount)
            .status(Order.OrderStatus.PENDING)
            .shippingAddress(request.getShippingAddress())
            .build();
        
        // Create order items (using effectively final reference)
        final Order orderRef = order;
        var orderItems = request.getItems().stream()
            .map(itemRequest -> {
                BigDecimal itemTotal = itemRequest.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
                
                return OrderItem.builder()
                    .order(orderRef)
                    .productId(itemRequest.getProductId())
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .totalPrice(itemTotal)
                    .build();
            })
            .collect(Collectors.toList());
        
        order.setItems(orderItems);
        
        // Save order (this will also save order items due to cascade)
        order = orderRepository.save(order);
        log.info("Order saved: {}", orderId);
        
        // Create event data
        OrderPlacedEventData eventData = OrderPlacedEventData.builder()
            .orderId(orderId)
            .customerId(request.getCustomerId())
            .totalAmount(totalAmount)
            .items(orderItems.stream()
                .map(item -> OrderPlacedEventData.OrderItemData.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getTotalPrice())
                    .build())
                .collect(Collectors.toList()))
            .shippingAddress(request.getShippingAddress())
            .build();
        
        // Write to outbox table (in the same transaction)
        // Debezium will capture this and publish to Kafka
        // Store simple event JSON with source, eventType, eventId, eventTime, and data
        try {
            String eventId = UUID.randomUUID().toString();
            String eventType = "com.ecommerce.order.placed";
            String source = "/order-service";
            java.time.OffsetDateTime eventTime = java.time.OffsetDateTime.now();
            
            // Create simple event wrapper structure
            Map<String, Object> eventWrapper = new HashMap<>();
            eventWrapper.put("source", source);
            eventWrapper.put("eventType", eventType);
            eventWrapper.put("eventId", eventId);
            eventWrapper.put("eventTime", eventTime.toString());
            eventWrapper.put("data", eventData);
            
            String eventJson = objectMapper.writeValueAsString(eventWrapper);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(orderId)
                .eventType(eventType)
                .source(source)
                .payload(eventJson) // Simple event JSON as string
                .correlationId(orderId) // Use order ID as correlation ID
                .build();
            
            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event written for order: {}", orderId);
            
        } catch (Exception e) {
            log.error("Failed to write outbox event for order: {}", orderId, e);
            throw new RuntimeException("Failed to create order event", e);
        }
        
        return OrderResponse.fromEntity(order);
    }
    
    /**
     * Retrieves an order by ID.
     */
    public OrderResponse getOrder(String orderId) {
        log.info("Retrieving order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        return OrderResponse.fromEntity(order);
    }
    
    /**
     * Updates order status (called by event consumers).
     */
    @Transactional
    public void updateOrderStatus(String orderId, Order.OrderStatus status) {
        log.info("Updating order status: orderId={}, status={}", orderId, status);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.setStatus(status);
        orderRepository.save(order);
        
        log.info("Order status updated: orderId={}, newStatus={}", orderId, status);
    }
    
    /**
     * Retrieves all orders for a customer.
     */
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        log.info("Retrieving orders for customer: {}", customerId);
        
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        
        return orders.stream()
            .map(OrderResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Retrieves all orders.
     */
    public List<OrderResponse> getAllOrders() {
        log.info("Retrieving all orders");
        
        List<Order> orders = orderRepository.findAll();
        
        return orders.stream()
            .map(OrderResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Creates multiple orders in batch (for load testing).
     */
    public List<OrderResponse> createBatchOrders(CreateOrderRequest templateRequest, int count) {
        log.info("Creating batch orders: count={}, customerId={}", count, templateRequest.getCustomerId());
        
        List<OrderResponse> orders = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            try {
                // Create a unique customer ID for each order in batch
                CreateOrderRequest request = new CreateOrderRequest(
                    templateRequest.getCustomerId() + "-batch-" + i,
                    templateRequest.getItems(),
                    templateRequest.getShippingAddress()
                );
                
                OrderResponse order = createOrder(request);
                orders.add(order);
                
                // Small delay to avoid overwhelming the system
                if (i % 10 == 0 && i > 0) {
                    try {
                        Thread.sleep(100); // 100ms delay every 10 orders
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Batch order creation interrupted");
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to create order in batch: index={}", i, e);
            }
        }
        
        log.info("Batch orders created: successful={}, total={}", orders.size(), count);
        return orders;
    }
}

