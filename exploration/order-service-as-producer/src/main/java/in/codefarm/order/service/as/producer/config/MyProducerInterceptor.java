package in.codefarm.order.service.as.producer.config;

import in.codefarm.order.service.as.producer.event.OrderPlacedEvent;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MyProducerInterceptor implements ProducerInterceptor<String, OrderPlacedEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(MyProducerInterceptor.class);
    
    @Override
    public ProducerRecord<String, OrderPlacedEvent> onSend(ProducerRecord<String, OrderPlacedEvent> record) {
        // Called before sending - can modify record
        log.info("Intercepting send - Topic: {}, Key: {}", record.topic(), record.key());
        
        // Add header
        record.headers().add("intercepted-at", String.valueOf(System.currentTimeMillis()).getBytes());
        
        return record;
    }
    
    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // Called after acknowledgment
        if (exception == null) {
            log.info("Message acknowledged - Partition: {}, Offset: {}", 
                metadata.partition(), 
                metadata.offset());
        } else {
            log.error("Message failed - Error: {}", exception.getMessage());
        }
    }
    
    @Override
    public void close() {
        // Cleanup
    }
    
    @Override
    public void configure(Map<String, ?> configs) {
        // Configure interceptor
    }
}