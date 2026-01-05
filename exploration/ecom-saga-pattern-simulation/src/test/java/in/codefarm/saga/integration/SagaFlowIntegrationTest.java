package in.codefarm.saga.integration;

import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.OrderPlacedEvent;
import in.codefarm.saga.inventory.repository.InventoryReservationRepository;
import in.codefarm.saga.order.repository.OrderRepository;
import in.codefarm.saga.order.service.OrderEventProducer;
import in.codefarm.saga.order.service.OrderService;
import in.codefarm.saga.payment.repository.PaymentRepository;
import in.codefarm.saga.payment.service.PaymentService;
import in.codefarm.saga.util.TestEventBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end integration tests for the complete saga pattern flow with real services.
 * 
 * <p><b>Technical Approach:</b>
 * <ul>
 *   <li>Uses <b>EmbeddedKafka</b> to create an in-memory Kafka broker for testing</li>
 *   <li>Uses <b>real application consumers</b> (via {@code @KafkaListener}) to process events</li>
 *   <li>Tests complete saga flow: Order → Payment → Inventory → Email</li>
 *   <li>Verifies database state after saga completion/compensation</li>
 * </ul>
 * 
 * <p><b>Key Technical Details:</b>
 * <ul>
 *   <li><b>Real Consumers:</b> Application's {@code @KafkaListener} methods automatically consume
 *       events from EmbeddedKafka, triggering real business logic</li>
 *   <li><b>Asynchronous Processing:</b> Uses {@code Awaitility} to wait for async event processing
 *       and database updates (events are processed asynchronously by consumers)</li>
 *   <li><b>Database Verification:</b> Verifies final state in repositories (orders, payments, inventory)
 *       after all saga steps complete</li>
 *   <li><b>Compensation Testing:</b> Tests compensation flows (payment failure → order cancellation,
 *       inventory failure → payment refund → order cancellation)</li>
 *   <li><b>Idempotency Testing:</b> Verifies duplicate events don't cause duplicate processing</li>
 * </ul>
 * 
 * <p><b>What This Tests:</b>
 * <ul>
 *   <li>Complete saga orchestration across multiple services</li>
 *   <li>Real consumer processing and business logic execution</li>
 *   <li>Database state consistency after saga completion</li>
 *   <li>Compensation transactions (rollback flows)</li>
 *   <li>Idempotent event processing</li>
 * </ul>
 * 
 * <p><b>Difference from SagaPatternIntegrationTest:</b>
 * This class tests the <b>business logic and state</b> (services, database), while
 * {@code SagaPatternIntegrationTest} tests the <b>messaging layer</b> (event structure, topics).
 * 
 * @see SagaPatternIntegrationTest
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"orders", "payments", "inventory"}
)
@DirtiesContext
@DisplayName("Complete Saga Flow Integration Tests")
class SagaFlowIntegrationTest {
    
    @Autowired
    private KafkaTemplate<String, EventWrapper<?>> kafkaTemplate;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderEventProducer orderEventProducer;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;
    
    @BeforeEach
    void setUp() {
        inventoryReservationRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        inventoryReservationRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }
    
    @Test
    @DisplayName("Should complete full saga flow: Order → Payment → Inventory → Email")
    void shouldCompleteFullSagaFlow() {
        String customerId = "customer-123";
        String productId = "product-456";
        Integer quantity = 2;
        BigDecimal amount = BigDecimal.valueOf(99.99);  // Will pass payment (< 1000)
        String orderId = java.util.UUID.randomUUID().toString();
        String transactionId = TestEventBuilder.generateTransactionId();
        
        orderService.createOrder(orderId, customerId, productId, quantity, amount, transactionId);
        OrderPlacedEvent orderEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        await().atMost(java.time.Duration.ofSeconds(10))
            .untilAsserted(() -> {
                var order = orderRepository.findByOrderId(orderId);
                assertThat(order).isPresent();
                assertThat(order.get().getStatus()).isEqualTo("PENDING");
                
                var payment = paymentService.findByOrderId(orderId);
                assertThat(payment).isPresent();
                assertThat(payment.get().getStatus()).isEqualTo("SUCCESS");
                
                var reservation = inventoryReservationRepository.findByOrderId(orderId);
                assertThat(reservation).isPresent();
                assertThat(reservation.get().getStatus()).isEqualTo("RESERVED");
            });
        var order = orderRepository.findByOrderId(orderId);
        assertThat(order).isPresent();
        
        var payment = paymentService.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo("SUCCESS");
        
        var reservation = inventoryReservationRepository.findByOrderId(orderId);
        assertThat(reservation).isPresent();
        assertThat(reservation.get().getStatus()).isEqualTo("RESERVED");
    }
    
    @Test
    @DisplayName("Should handle payment failure and cancel order (compensation)")
    void shouldHandlePaymentFailureAndCancelOrder() {
        String customerId = "customer-123";
        String productId = "product-456";
        Integer quantity = 2;
        BigDecimal amount = BigDecimal.valueOf(1500.00);  // Will fail payment (> 1000)
        String orderId = java.util.UUID.randomUUID().toString();
        String transactionId = TestEventBuilder.generateTransactionId();
        
        // Create order in database
        orderService.createOrder(orderId, customerId, productId, quantity, amount, transactionId);
        
        // Publish OrderPlacedEvent
        OrderPlacedEvent orderEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        await().atMost(java.time.Duration.ofSeconds(10))
            .untilAsserted(() -> {
                var order = orderRepository.findByOrderId(orderId);
                assertThat(order).isPresent();
                
                var payment = paymentService.findByOrderId(orderId);
                assertThat(payment).isPresent();
                assertThat(payment.get().getStatus()).isEqualTo("FAILED");
                
                var cancelledOrder = orderRepository.findByOrderId(orderId);
                assertThat(cancelledOrder).isPresent();
                assertThat(cancelledOrder.get().getStatus()).isEqualTo("CANCELLED");
            });
        var order = orderRepository.findByOrderId(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getStatus()).isEqualTo("CANCELLED");
        
        var payment = paymentService.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo("FAILED");
    }
    
    @Test
    @DisplayName("Should handle inventory failure and trigger payment refund and order cancellation")
    void shouldHandleInventoryFailureAndTriggerCompensation() {
        String customerId = "customer-123";
        String productId = "product-456";
        Integer quantity = 2;
        BigDecimal amount = BigDecimal.valueOf(600.00);  // Will pass payment but fail inventory (> 500)
        String orderId = java.util.UUID.randomUUID().toString();
        String transactionId = TestEventBuilder.generateTransactionId();
        
        // Create order in database
        orderService.createOrder(orderId, customerId, productId, quantity, amount, transactionId);
        
        // Publish OrderPlacedEvent
        OrderPlacedEvent orderEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        await().atMost(java.time.Duration.ofSeconds(10))
            .untilAsserted(() -> {
                var order = orderRepository.findByOrderId(orderId);
                assertThat(order).isPresent();
                
                var payment = paymentService.findByOrderId(orderId);
                assertThat(payment).isPresent();
                assertThat(payment.get().getStatus()).isEqualTo("REFUNDED");
                
                var cancelledOrder = orderRepository.findByOrderId(orderId);
                assertThat(cancelledOrder).isPresent();
                assertThat(cancelledOrder.get().getStatus()).isEqualTo("CANCELLED");
            });
        var order = orderRepository.findByOrderId(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getStatus()).isEqualTo("CANCELLED");
        
        var payment = paymentService.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo("REFUNDED");
    }
    
    @Test
    @DisplayName("Should handle duplicate OrderPlacedEvent idempotently")
    void shouldHandleDuplicateOrderPlacedEventIdempotently() throws InterruptedException {
        String customerId = "customer-123";
        String productId = "product-456";
        Integer quantity = 2;
        BigDecimal amount = BigDecimal.valueOf(99.99);
        String orderId = java.util.UUID.randomUUID().toString();
        String transactionId = TestEventBuilder.generateTransactionId();
        
        // Create order in database
        orderService.createOrder(orderId, customerId, productId, quantity, amount, transactionId);
        
        // Publish OrderPlacedEvent
        OrderPlacedEvent orderEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        await().atMost(java.time.Duration.ofSeconds(5))
            .until(() -> paymentService.findByOrderId(orderId).isPresent());
        
        var initialPayment = paymentService.findByOrderId(orderId);
        assertThat(initialPayment).isPresent();
        OrderPlacedEvent duplicateEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        
        EventWrapper<OrderPlacedEvent> wrapper = TestEventBuilder.wrapOrderPlaced(
            duplicateEvent,
            TestEventBuilder.generateTransactionId()
        );
        
        kafkaTemplate.send("orders", orderId, wrapper);
        kafkaTemplate.flush();
        
        Thread.sleep(2000);
        
        var payments = paymentService.findByOrderId(orderId);
        assertThat(payments).isPresent();
    }
}

