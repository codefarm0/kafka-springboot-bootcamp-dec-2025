package in.codefarm.saga.order.consumer;

import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.PaymentProcessedEvent;
import in.codefarm.saga.event.PaymentRefundedEvent;
import in.codefarm.saga.event.OrderCancelledEvent;
import in.codefarm.saga.order.service.OrderService;
import in.codefarm.saga.order.service.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@Component
public class OrderServiceConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(OrderServiceConsumer.class);
    
    private final OrderService orderService;
    private final OrderEventProducer orderEventProducer;
    private final JsonMapper jsonMapper;
    
    public OrderServiceConsumer(
        OrderService orderService, 
        OrderEventProducer orderEventProducer,
        @Qualifier("consumerJsonMapper") JsonMapper jsonMapper
    ) {
        this.orderService = orderService;
        this.orderEventProducer = orderEventProducer;
        this.jsonMapper = jsonMapper;
    }
    
    @KafkaListener(
        topics = "payments",
        groupId = "order-service-compensation-group",
        containerFactory = "eventWrapperKafkaListenerContainerFactory"
    )
    public void handlePaymentEvents(@Payload EventWrapper<?> wrapper) {
        try {
            String eventType = wrapper.metadata().eventType();
            String transactionId = wrapper.metadata().transactionId();
            
            // Handle PaymentProcessedEvent with FAILED status (compensation for payment failure)
            if ("PaymentProcessed".equals(eventType)) {
                PaymentProcessedEvent event = jsonMapper.convertValue(wrapper.payload(), PaymentProcessedEvent.class);
                
                // Only cancel order if payment failed
                if ("FAILED".equals(event.status())) {
                    log.info("Order Service: Received PaymentProcessed (FAILED) event - OrderId: {}, TransactionId: {}", 
                        event.orderId(), transactionId);
                    
                    // Cancel order as compensation for failed payment
                    // No need to publish OrderCancelledEvent - compensation is complete
                    orderService.cancelOrder(event.orderId(), "Payment failed");
                    
                    log.info("Order Service: Order cancelled due to payment failure - OrderId: {}, TransactionId: {}", 
                        event.orderId(), transactionId);
                } else {
                    log.debug("Order Service: Ignoring PaymentProcessed (SUCCESS) event - OrderId: {}", event.orderId());
                }
                return;
            }
            
            // Handle PaymentRefunded event (compensation for inventory unavailability)
            if ("PaymentRefunded".equals(eventType)) {
                PaymentRefundedEvent event = jsonMapper.convertValue(wrapper.payload(), PaymentRefundedEvent.class);
                
                log.info("Order Service: Received PaymentRefunded event - OrderId: {}, TransactionId: {}", 
                    event.orderId(), transactionId);
                
                // Cancel order as compensation
                orderService.cancelOrder(event.orderId(), "Payment refunded");
                
                // Publish order cancelled event
                var cancelledEvent = new OrderCancelledEvent(
                    event.orderId(),
                    "Payment refunded",
                    LocalDateTime.now()
                );
                
                orderEventProducer.sendOrderCancelledEvent(cancelledEvent, transactionId);
                
                log.info("Order Service: Order cancelled - OrderId: {}, TransactionId: {}", 
                    event.orderId(), transactionId);
                return;
            }
            
            log.debug("Order Service: Ignoring event type: {}", eventType);
        } catch (Exception e) {
            log.error("Order Service: Error handling payment event", e);
        }
    }
}

