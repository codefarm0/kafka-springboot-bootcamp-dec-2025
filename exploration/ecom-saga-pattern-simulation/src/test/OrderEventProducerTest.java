package in.codefarm.saga.unit;

import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.OrderPlacedEvent;
import in.codefarm.saga.order.service.OrderEventProducer;
import in.codefarm.saga.testutil.TestEventBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Event Producer Unit Tests")
class OrderEventProducerTest {
    
    @Mock
    private KafkaTemplate<String, EventWrapper<?>> kafkaTemplate;
    
    @InjectMocks
    private OrderEventProducer orderEventProducer;
    
    private ArgumentCaptor<EventWrapper<?>> eventWrapperCaptor;
    private ArgumentCaptor<String> topicCaptor;
    private ArgumentCaptor<String> keyCaptor;
    
    @BeforeEach
    void setUp() {
        eventWrapperCaptor = ArgumentCaptor.forClass(EventWrapper.class);
        topicCaptor = ArgumentCaptor.forClass(String.class);
        keyCaptor = ArgumentCaptor.forClass(String.class);
        
        SendResult<String, EventWrapper<?>> sendResult = new SendResult<>(null, null);
        lenient().when(kafkaTemplate.send(anyString(), anyString(), any(EventWrapper.class)))
            .thenReturn(CompletableFuture.completedFuture(sendResult));
    }
    
    @Test
    @DisplayName("Should send OrderPlacedEvent with correct metadata and topic")
    void shouldSendOrderPlacedEventWithCorrectMetadata() {
        String transactionId = TestEventBuilder.generateTransactionId();
        OrderPlacedEvent orderEvent = TestEventBuilder.defaultOrderPlacedEvent();
        
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        verify(kafkaTemplate, times(1)).send(
            topicCaptor.capture(),
            keyCaptor.capture(),
            eventWrapperCaptor.capture()
        );
        
        assertThat(topicCaptor.getValue()).isEqualTo("orders");
        assertThat(keyCaptor.getValue()).isEqualTo(orderEvent.orderId());
        
        EventWrapper<?> capturedWrapper = eventWrapperCaptor.getValue();
        assertThat(capturedWrapper.metadata().eventType()).isEqualTo("OrderPlaced");
        assertThat(capturedWrapper.metadata().eventVersion()).isEqualTo("1.0");
        assertThat(capturedWrapper.metadata().source()).isEqualTo("order-service");
        assertThat(capturedWrapper.metadata().transactionId()).isEqualTo(transactionId);
        assertThat(capturedWrapper.payload()).isInstanceOf(OrderPlacedEvent.class);
    }
    
    @Test
    @DisplayName("Should handle Kafka send failure gracefully")
    void shouldHandleSendFailureGracefully() {
        when(kafkaTemplate.send(anyString(), anyString(), any(EventWrapper.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka send failed")));
        
        String transactionId = TestEventBuilder.generateTransactionId();
        OrderPlacedEvent orderEvent = TestEventBuilder.defaultOrderPlacedEvent();
        
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(EventWrapper.class));
    }
    
    @Test
    @DisplayName("Should send OrderCancelledEvent with correct metadata")
    void shouldSendOrderCancelledEventWithCorrectMetadata() {
        String transactionId = TestEventBuilder.generateTransactionId();
        String orderId = "order-123";
        String reason = "Payment failed";
        
        orderEventProducer.sendOrderCancelledEvent(
            TestEventBuilder.orderCancelled(orderId, reason),
            transactionId
        );
        
        verify(kafkaTemplate, times(1)).send(
            topicCaptor.capture(),
            keyCaptor.capture(),
            eventWrapperCaptor.capture()
        );
        
        assertThat(topicCaptor.getValue()).isEqualTo("orders");
        assertThat(keyCaptor.getValue()).isEqualTo(orderId);
        
        EventWrapper<?> capturedWrapper = eventWrapperCaptor.getValue();
        assertThat(capturedWrapper.metadata().eventType()).isEqualTo("OrderCancelled");
        assertThat(capturedWrapper.metadata().source()).isEqualTo("order-service");
        assertThat(capturedWrapper.metadata().transactionId()).isEqualTo(transactionId);
    }
}

