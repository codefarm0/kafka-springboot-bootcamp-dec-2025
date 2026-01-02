package in.codefarm.saga.order.service;

import in.codefarm.saga.order.entity.OrderEntity;
import in.codefarm.saga.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    @Transactional
    public OrderEntity createOrder(String orderId, String customerId, String productId,
                                   Integer quantity, java.math.BigDecimal totalAmount,
                                   String transactionId) {
        var order = new OrderEntity(
            orderId,
            customerId,
            productId,
            quantity,
            totalAmount,
            java.time.LocalDateTime.now(),
            transactionId
        );
        
        return orderRepository.save(order);
    }
    
    public Optional<OrderEntity> findByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }
    
    public List<OrderEntity> findAll() {
        return orderRepository.findAll();
    }
    
    @Transactional
    public void cancelOrder(String orderId, String reason) {
        orderRepository.findByOrderId(orderId).ifPresent(order -> {
            // Idempotency check: skip if already cancelled
            if ("CANCELLED".equals(order.getStatus())) {
                log.warn("Order {} is already cancelled - skipping (idempotency). Reason: {}", orderId, reason);
                return;
            }
            
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            log.info("Order {} cancelled. Reason: {}", orderId, reason);
        });
    }
}

