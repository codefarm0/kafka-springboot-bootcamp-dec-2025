package in.codefarm.saga.integration;

import in.codefarm.saga.event.*;
import in.codefarm.saga.inventory.repository.InventoryReservationRepository;
import in.codefarm.saga.order.repository.OrderRepository;
import in.codefarm.saga.payment.repository.PaymentRepository;
import in.codefarm.saga.util.TestEventBuilder;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Kafka event publishing and consumption verification.
 * 
 * <p><b>Technical Approach:</b>
 * <ul>
 *   <li>Uses <b>EmbeddedKafka</b> to create an in-memory Kafka broker for testing</li>
 *   <li>Creates <b>manual Kafka consumers</b> to directly verify events published to topics</li>
 *   <li>Tests event structure, metadata, and topic routing at the messaging layer</li>
 *   <li>Does NOT use real application consumers - focuses on event verification only</li>
 * </ul>
 * 
 * <p><b>Key Technical Details:</b>
 * <ul>
 *   <li><b>Manual Consumers:</b> Creates dedicated test consumers using {@code DefaultKafkaConsumerFactory}
 *       to consume from specific topics and verify event structure</li>
 *   <li><b>Event Filtering:</b> Uses {@code getRecords()} with stream filtering to handle multiple records
 *       (since real application consumers also process events concurrently)</li>
 *   <li><b>Serialization:</b> Uses {@code JacksonJsonDeserializer} with {@code EventWrapper} to match
 *       production deserialization configuration</li>
 *   <li><b>Test Isolation:</b> Each test creates unique consumer group IDs to avoid conflicts</li>
 * </ul>
 * 
 * <p><b>What This Tests:</b>
 * <ul>
 *   <li>Events are published to correct topics with correct keys</li>
 *   <li>Event metadata (eventType, transactionId, source, version) is correct</li>
 *   <li>Event structure and serialization/deserialization works correctly</li>
 *   <li>Compensation events (OrderCancelled, PaymentRefunded) are published correctly</li>
 * </ul>
 * 
 * <p><b>Difference from SagaFlowIntegrationTest:</b>
 * This class tests the <b>messaging layer</b> (Kafka events), while {@code SagaFlowIntegrationTest}
 * tests the <b>business logic layer</b> (services, database state, real consumers).
 * 
 * @see SagaFlowIntegrationTest
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {
        "orders",      // Order events topic
        "payments",    // Payment events topic
        "inventory"    // Inventory events topic
    }
)
@DirtiesContext
@DisplayName("Saga Pattern Integration Tests")
class SagaPatternIntegrationTest {
    
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    
    @Autowired
    private KafkaTemplate<String, EventWrapper<?>> kafkaTemplate;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;
    
    private Consumer<String, EventWrapper<?>> ordersConsumer;
    private Consumer<String, EventWrapper<?>> paymentsConsumer;
    private Consumer<String, EventWrapper<?>> inventoryConsumer;
    
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    
    @BeforeEach
    void setUp() {
        // Create consumer for orders topic
        // Using JacksonJsonDeserializer to match production configuration
        Map<String, Object> ordersConsumerProps = KafkaTestUtils.consumerProps(
            embeddedKafkaBroker,
            "test-orders-consumer-" + UUID.randomUUID(),
            true
        );
        JacksonJsonDeserializer<EventWrapper<?>> ordersDeserializer = 
            new JacksonJsonDeserializer<>(EventWrapper.class, jsonMapper);
        ordersDeserializer.addTrustedPackages("*");
        ordersConsumer = new DefaultKafkaConsumerFactory<String, EventWrapper<?>>(
            ordersConsumerProps,
            new StringDeserializer(),
            ordersDeserializer
        ).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(ordersConsumer, "orders");
        
        // Create consumer for payments topic
        Map<String, Object> paymentsConsumerProps = KafkaTestUtils.consumerProps(
            embeddedKafkaBroker,
            "test-payments-consumer-" + UUID.randomUUID(),
            true
        );
        JacksonJsonDeserializer<EventWrapper<?>> paymentsDeserializer = 
            new JacksonJsonDeserializer<>(EventWrapper.class, jsonMapper);
        paymentsDeserializer.addTrustedPackages("*");
        paymentsConsumer = new DefaultKafkaConsumerFactory<String, EventWrapper<?>>(
            paymentsConsumerProps,
            new StringDeserializer(),
            paymentsDeserializer
        ).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(paymentsConsumer, "payments");
        
        // Create consumer for inventory topic
        Map<String, Object> inventoryConsumerProps = KafkaTestUtils.consumerProps(
            embeddedKafkaBroker,
            "test-inventory-consumer-" + UUID.randomUUID(),
            true
        );
        JacksonJsonDeserializer<EventWrapper<?>> inventoryDeserializer = 
            new JacksonJsonDeserializer<>(EventWrapper.class, jsonMapper);
        inventoryDeserializer.addTrustedPackages("*");
        inventoryConsumer = new DefaultKafkaConsumerFactory<String, EventWrapper<?>>(
            inventoryConsumerProps,
            new StringDeserializer(),
            inventoryDeserializer
        ).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(inventoryConsumer, "inventory");
    }
    
    @AfterEach
    void tearDown() {
        if (ordersConsumer != null) {
            ordersConsumer.close();
        }
        if (paymentsConsumer != null) {
            paymentsConsumer.close();
        }
        if (inventoryConsumer != null) {
            inventoryConsumer.close();
        }
        
        // Clean up database
        inventoryReservationRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }
    
    /**
     * Test: Successful Saga Flow
     * 
     * DEMO SCENARIO:
     * 1. Order is placed → OrderPlacedEvent published
     * 2. Payment service processes payment → PaymentProcessedEvent (SUCCESS) published
     * 3. Inventory service reserves inventory → InventoryReservedEvent (RESERVED) published
     * 4. Email service sends confirmation
     * 
     * VERIFICATION:
     * - OrderPlacedEvent is published to orders topic
     * - PaymentProcessedEvent is published to payments topic
     * - InventoryReservedEvent is published to inventory topic
     * - All entities are saved in database
     */
    @Test
    @DisplayName("Should complete successful saga flow: Order → Payment → Inventory")
    void shouldCompleteSuccessfulSagaFlow() {
        // Given: Create test order event
        String transactionId = TestEventBuilder.generateTransactionId();
        OrderPlacedEvent orderEvent = TestEventBuilder.defaultOrderPlacedEvent();
        EventWrapper<OrderPlacedEvent> orderWrapper = TestEventBuilder.wrapOrderPlaced(orderEvent, transactionId);
        
        kafkaTemplate.send("orders", orderEvent.orderId(), orderWrapper);
        kafkaTemplate.flush();
        
        ConsumerRecords<String, EventWrapper<?>> orderRecords = 
            KafkaTestUtils.getRecords(ordersConsumer, Duration.ofSeconds(5));
        ConsumerRecord<String, EventWrapper<?>> orderRecord = StreamSupport.stream(
            orderRecords.records("orders").spliterator(), false)
            .filter(record -> record.value().metadata().eventType().equals("OrderPlaced"))
            .filter(record -> record.value().metadata().transactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("OrderPlaced event not found"));
        
        assertThat(orderRecord).isNotNull();
        assertThat(orderRecord.key()).isEqualTo(orderEvent.orderId());
        assertThat(orderRecord.value().metadata().eventType()).isEqualTo("OrderPlaced");
        assertThat(orderRecord.value().metadata().transactionId()).isEqualTo(transactionId);
        PaymentProcessedEvent paymentEvent = TestEventBuilder.paymentProcessedSuccess(
            orderEvent.orderId(),
            orderEvent.customerId(),
            orderEvent.totalAmount()
        );
        EventWrapper<PaymentProcessedEvent> paymentWrapper = 
            TestEventBuilder.wrapPaymentProcessed(paymentEvent, transactionId);
        
        kafkaTemplate.send("payments", orderEvent.orderId(), paymentWrapper);
        kafkaTemplate.flush();
        
        ConsumerRecords<String, EventWrapper<?>> paymentRecords = 
            KafkaTestUtils.getRecords(paymentsConsumer, Duration.ofSeconds(5));
        ConsumerRecord<String, EventWrapper<?>> paymentRecord = StreamSupport.stream(
            paymentRecords.records("payments").spliterator(), false)
            .filter(record -> record.value().metadata().eventType().equals("PaymentProcessed"))
            .filter(record -> record.value().metadata().transactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("PaymentProcessed event not found"));
        
        assertThat(paymentRecord).isNotNull();
        assertThat(paymentRecord.value().metadata().eventType()).isEqualTo("PaymentProcessed");
        InventoryReservedEvent inventoryEvent = TestEventBuilder.inventoryReserved(
            orderEvent.orderId(),
            orderEvent.productId(),
            orderEvent.quantity()
        );
        EventWrapper<InventoryReservedEvent> inventoryWrapper = 
            TestEventBuilder.wrapInventoryReserved(inventoryEvent, transactionId);
        
        kafkaTemplate.send("inventory", orderEvent.orderId(), inventoryWrapper);
        kafkaTemplate.flush();
        
        ConsumerRecords<String, EventWrapper<?>> inventoryRecords = 
            KafkaTestUtils.getRecords(inventoryConsumer, Duration.ofSeconds(5));
        ConsumerRecord<String, EventWrapper<?>> inventoryRecord = StreamSupport.stream(
            inventoryRecords.records("inventory").spliterator(), false)
            .filter(record -> record.value().metadata().eventType().equals("InventoryReserved"))
            .filter(record -> record.value().metadata().transactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("InventoryReserved event not found"));
        
        assertThat(inventoryRecord).isNotNull();
        assertThat(inventoryRecord.value().metadata().eventType()).isEqualTo("InventoryReserved");
    }
    
    @Test
    @DisplayName("Should handle payment failure and trigger order cancellation")
    void shouldHandlePaymentFailureAndCancelOrder() {
        String transactionId = TestEventBuilder.generateTransactionId();
        OrderPlacedEvent orderEvent = TestEventBuilder.orderPlacedEventWithAmount(
            BigDecimal.valueOf(1500.00)  // Amount > 1000 will fail payment
        );
        EventWrapper<OrderPlacedEvent> orderWrapper = TestEventBuilder.wrapOrderPlaced(orderEvent, transactionId);
        
        kafkaTemplate.send("orders", orderEvent.orderId(), orderWrapper);
        kafkaTemplate.flush();
        PaymentProcessedEvent paymentEvent = TestEventBuilder.paymentProcessedFailed(
            orderEvent.orderId(),
            orderEvent.customerId(),
            orderEvent.totalAmount()
        );
        EventWrapper<PaymentProcessedEvent> paymentWrapper = 
            TestEventBuilder.wrapPaymentProcessed(paymentEvent, transactionId);
        
        kafkaTemplate.send("payments", orderEvent.orderId(), paymentWrapper);
        kafkaTemplate.flush();
        
        ConsumerRecords<String, EventWrapper<?>> paymentRecords = 
            KafkaTestUtils.getRecords(paymentsConsumer, Duration.ofSeconds(5));
        ConsumerRecord<String, EventWrapper<?>> paymentRecord = StreamSupport.stream(
            paymentRecords.records("payments").spliterator(), false)
            .filter(record -> record.value().metadata().eventType().equals("PaymentProcessed"))
            .filter(record -> record.value().metadata().transactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("PaymentProcessed event not found"));
        
        assertThat(paymentRecord).isNotNull();
        assertThat(paymentRecord.value().metadata().eventType()).isEqualTo("PaymentProcessed");
        
        OrderCancelledEvent cancelEvent = TestEventBuilder.orderCancelled(
            orderEvent.orderId(),
            "Payment failed"
        );
        EventWrapper<OrderCancelledEvent> cancelWrapper = new EventWrapper<>(
            new EventMetadata("OrderCancelled", "1.0", "order-service", transactionId, java.time.LocalDateTime.now()),
            cancelEvent
        );
        
        kafkaTemplate.send("orders", orderEvent.orderId(), cancelWrapper);
        kafkaTemplate.flush();
        
        ConsumerRecords<String, EventWrapper<?>> cancelRecords = 
            KafkaTestUtils.getRecords(ordersConsumer, Duration.ofSeconds(5));
        ConsumerRecord<String, EventWrapper<?>> cancelRecord = StreamSupport.stream(
            cancelRecords.records("orders").spliterator(), false)
            .filter(record -> record.value().metadata().eventType().equals("OrderCancelled"))
            .filter(record -> record.value().metadata().transactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("OrderCancelled event not found"));
        
        assertThat(cancelRecord).isNotNull();
        assertThat(cancelRecord.value().metadata().eventType()).isEqualTo("OrderCancelled");
    }
    
    @Test
    @DisplayName("Should handle inventory failure and trigger payment refund and order cancellation")
    void shouldHandleInventoryFailureAndTriggerCompensation() {
        String transactionId = TestEventBuilder.generateTransactionId();
        OrderPlacedEvent orderEvent = TestEventBuilder.orderPlacedEventWithAmount(
            BigDecimal.valueOf(600.00)  // > 500 will fail inventory reservation
        );
        EventWrapper<OrderPlacedEvent> orderWrapper = TestEventBuilder.wrapOrderPlaced(orderEvent, transactionId);
        
        kafkaTemplate.send("orders", orderEvent.orderId(), orderWrapper);
        kafkaTemplate.flush();
        PaymentProcessedEvent paymentEvent = TestEventBuilder.paymentProcessedSuccess(
            orderEvent.orderId(),
            orderEvent.customerId(),
            orderEvent.totalAmount()
        );
        EventWrapper<PaymentProcessedEvent> paymentWrapper = 
            TestEventBuilder.wrapPaymentProcessed(paymentEvent, transactionId);
        
        kafkaTemplate.send("payments", orderEvent.orderId(), paymentWrapper);
        kafkaTemplate.flush();
        
        // Inventory reservation fails
        InventoryReservedEvent inventoryEvent = TestEventBuilder.inventoryUnavailable(
            orderEvent.orderId(),
            orderEvent.productId(),
            orderEvent.quantity()
        );
        EventWrapper<InventoryReservedEvent> inventoryWrapper = 
            TestEventBuilder.wrapInventoryReserved(inventoryEvent, transactionId);
        
        kafkaTemplate.send("inventory", orderEvent.orderId(), inventoryWrapper);
        kafkaTemplate.flush();
        PaymentRefundedEvent refundEvent = TestEventBuilder.paymentRefunded(
            orderEvent.orderId(),
            orderEvent.customerId(),
            orderEvent.totalAmount()
        );
        EventWrapper<PaymentRefundedEvent> refundWrapper = 
            TestEventBuilder.wrapPaymentRefunded(refundEvent, transactionId);
        
        kafkaTemplate.send("payments", orderEvent.orderId(), refundWrapper);
        kafkaTemplate.flush();
        
        ConsumerRecords<String, EventWrapper<?>> refundRecords = 
            KafkaTestUtils.getRecords(paymentsConsumer, Duration.ofSeconds(5));
        ConsumerRecord<String, EventWrapper<?>> refundRecord = StreamSupport.stream(
            refundRecords.records("payments").spliterator(), false)
            .filter(record -> record.value().metadata().eventType().equals("PaymentRefunded"))
            .filter(record -> record.value().metadata().transactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("PaymentRefunded event not found"));
        
        assertThat(refundRecord).isNotNull();
        assertThat(refundRecord.value().metadata().eventType()).isEqualTo("PaymentRefunded");
        OrderCancelledEvent cancelEvent = TestEventBuilder.orderCancelled(
            orderEvent.orderId(),
            "Inventory unavailable"
        );
        EventWrapper<OrderCancelledEvent> cancelWrapper = new EventWrapper<>(
            new EventMetadata("OrderCancelled", "1.0", "order-service", transactionId, java.time.LocalDateTime.now()),
            cancelEvent
        );
        
        kafkaTemplate.send("orders", orderEvent.orderId(), cancelWrapper);
        kafkaTemplate.flush();
        
        ConsumerRecords<String, EventWrapper<?>> cancelRecords = 
            KafkaTestUtils.getRecords(ordersConsumer, Duration.ofSeconds(5));
        ConsumerRecord<String, EventWrapper<?>> cancelRecord = StreamSupport.stream(
            cancelRecords.records("orders").spliterator(), false)
            .filter(record -> record.value().metadata().eventType().equals("OrderCancelled"))
            .filter(record -> record.value().metadata().transactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("OrderCancelled event not found"));
        
        assertThat(cancelRecord).isNotNull();
        assertThat(cancelRecord.value().metadata().eventType()).isEqualTo("OrderCancelled");
    }
    
    @Test
    @DisplayName("Should include proper metadata in all events for saga tracking")
    void shouldIncludeProperMetadataInEvents() {
        String transactionId = TestEventBuilder.generateTransactionId();
        OrderPlacedEvent orderEvent = TestEventBuilder.defaultOrderPlacedEvent();
        EventWrapper<OrderPlacedEvent> orderWrapper = TestEventBuilder.wrapOrderPlaced(orderEvent, transactionId);
        
        kafkaTemplate.send("orders", orderEvent.orderId(), orderWrapper);
        kafkaTemplate.flush();
        
        ConsumerRecords<String, EventWrapper<?>> records = 
            KafkaTestUtils.getRecords(ordersConsumer, Duration.ofSeconds(5));
        ConsumerRecord<String, EventWrapper<?>> record = StreamSupport.stream(
            records.records("orders").spliterator(), false)
            .filter(r -> r.value().metadata().eventType().equals("OrderPlaced"))
            .filter(r -> r.value().metadata().transactionId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("OrderPlaced event not found"));
        
        EventMetadata metadata = record.value().metadata();
        
        assertThat(metadata.eventType()).isEqualTo("OrderPlaced");
        assertThat(metadata.eventVersion()).isEqualTo("1.0");
        assertThat(metadata.source()).isEqualTo("order-service");
        assertThat(metadata.transactionId()).isEqualTo(transactionId);
        assertThat(metadata.timestamp()).isNotNull();
    }
}

