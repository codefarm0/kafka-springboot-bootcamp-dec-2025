package in.codefarm.saga.unit;

import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.PaymentProcessedEvent;
import in.codefarm.saga.event.PaymentRefundedEvent;
import in.codefarm.saga.payment.service.PaymentEventProducer;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Event Producer Unit Tests")
class PaymentEventProducerTest {
    
    private static final String TEST_ORDER_ID = "order-123";
    private static final String TEST_CUSTOMER_ID = "customer-456";
    
    @Mock
    private KafkaTemplate<String, EventWrapper<?>> kafkaTemplate;
    
    @InjectMocks
    private PaymentEventProducer paymentEventProducer;
    
    private ArgumentCaptor<EventWrapper<?>> eventWrapperCaptor;
    private ArgumentCaptor<String> topicCaptor;
    
    @BeforeEach
    void setUp() {
        eventWrapperCaptor = ArgumentCaptor.forClass(EventWrapper.class);
        topicCaptor = ArgumentCaptor.forClass(String.class);
        
        SendResult<String, EventWrapper<?>> sendResult = new SendResult<>(null, null);
        when(kafkaTemplate.send(anyString(), anyString(), any(EventWrapper.class)))
            .thenReturn(CompletableFuture.completedFuture(sendResult));
    }
    
    @Test
    @DisplayName("Should send PaymentProcessedEvent with SUCCESS status")
    void shouldSendPaymentProcessedEventSuccess() {
        String transactionId = TestEventBuilder.generateTransactionId();
        PaymentProcessedEvent paymentEvent = TestEventBuilder.paymentProcessedSuccess(
            TEST_ORDER_ID,
            TEST_CUSTOMER_ID,
            BigDecimal.valueOf(99.99)
        );
        
        paymentEventProducer.sendPaymentProcessedEvent(paymentEvent, transactionId);
        
        verify(kafkaTemplate, times(1)).send(
            topicCaptor.capture(),
            anyString(),
            eventWrapperCaptor.capture()
        );
        
        assertThat(topicCaptor.getValue()).isEqualTo("payments");
        
        EventWrapper<?> capturedWrapper = eventWrapperCaptor.getValue();
        assertThat(capturedWrapper.metadata().eventType()).isEqualTo("PaymentProcessed");
        assertThat(capturedWrapper.metadata().source()).isEqualTo("payment-service");
        assertThat(capturedWrapper.metadata().transactionId()).isEqualTo(transactionId);
    }
    
    @Test
    @DisplayName("Should send PaymentProcessedEvent with FAILED status")
    void shouldSendPaymentProcessedEventFailed() {
        String transactionId = TestEventBuilder.generateTransactionId();
        PaymentProcessedEvent paymentEvent = TestEventBuilder.paymentProcessedFailed(
            TEST_ORDER_ID,
            TEST_CUSTOMER_ID,
            BigDecimal.valueOf(1500.00)
        );
        
        paymentEventProducer.sendPaymentProcessedEvent(paymentEvent, transactionId);
        
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), eventWrapperCaptor.capture());
        
        EventWrapper<?> capturedWrapper = eventWrapperCaptor.getValue();
        assertThat(capturedWrapper.metadata().eventType()).isEqualTo("PaymentProcessed");
        
        PaymentProcessedEvent capturedEvent = (PaymentProcessedEvent) capturedWrapper.payload();
        assertThat(capturedEvent.status()).isEqualTo("FAILED");
    }
    
    @Test
    @DisplayName("Should send PaymentRefundedEvent for compensation")
    void shouldSendPaymentRefundedEvent() {
        String transactionId = TestEventBuilder.generateTransactionId();
        PaymentRefundedEvent refundEvent = TestEventBuilder.paymentRefunded(
            TEST_ORDER_ID,
            TEST_CUSTOMER_ID,
            BigDecimal.valueOf(99.99)
        );
        
        paymentEventProducer.sendPaymentRefundedEvent(refundEvent, transactionId);
        
        verify(kafkaTemplate, times(1)).send(
            topicCaptor.capture(),
            anyString(),
            eventWrapperCaptor.capture()
        );
        
        assertThat(topicCaptor.getValue()).isEqualTo("payments");
        
        EventWrapper<?> capturedWrapper = eventWrapperCaptor.getValue();
        assertThat(capturedWrapper.metadata().eventType()).isEqualTo("PaymentRefunded");
        assertThat(capturedWrapper.metadata().source()).isEqualTo("payment-service");
    }
}

