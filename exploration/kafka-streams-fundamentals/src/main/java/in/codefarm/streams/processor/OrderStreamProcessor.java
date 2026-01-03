package in.codefarm.streams.processor;

import in.codefarm.streams.event.EnrichedOrderEvent;
import in.codefarm.streams.event.OrderPlacedEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class OrderStreamProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(OrderStreamProcessor.class);
    private static final BigDecimal HIGH_VALUE_THRESHOLD = BigDecimal.valueOf(100);
    
    @Bean
    public KStream<String, EnrichedOrderEvent> processOrderStream(StreamsBuilder streamsBuilder) {
        // Read from orders topic
        KStream<String, OrderPlacedEvent> orders = streamsBuilder.stream(
            "orders",
            Consumed.with(Serdes.String(), new JacksonJsonSerde<>(OrderPlacedEvent.class))
        );
        
        log.info("Order stream processor initialized");
        
        // Transform: Enrich orders with additional information
        KStream<String, EnrichedOrderEvent> enrichedOrders = orders.mapValues(order -> {
            String category = order.totalAmount().compareTo(HIGH_VALUE_THRESHOLD) >= 0 
                ? "HIGH_VALUE" 
                : "REGULAR";
            
            return new EnrichedOrderEvent(
                order.orderId(),
                order.customerId(),
                order.productId(),
                order.quantity(),
                order.totalAmount(),
                order.orderDate(),
                category,
                LocalDateTime.now(),
                "PROCESSED"
            );
        });
        
        // Filter: Keep only valid orders
        KStream<String, EnrichedOrderEvent> validOrders = enrichedOrders.filter((key, order) -> {
            boolean isValid = order.quantity() != null 
                && order.quantity() > 0 
                && order.totalAmount() != null
                && order.totalAmount().compareTo(BigDecimal.ZERO) > 0;
            
            if (!isValid) {
                log.warn("Filtered out invalid order: {}", order.orderId());
            }
            
            return isValid;
        });
        
        // Split into high-value and regular using filter
        KStream<String, EnrichedOrderEvent> highValueOrders = validOrders.filter(
            (key, order) -> "HIGH_VALUE".equals(order.orderCategory())
        );
        
        KStream<String, EnrichedOrderEvent> regularOrders = validOrders.filter(
            (key, order) -> "REGULAR".equals(order.orderCategory())
        );
        
        // Write high-value orders
        highValueOrders.to(
            "high-value-orders",
            Produced.with(Serdes.String(), new JacksonJsonSerde<>(EnrichedOrderEvent.class))
        );
        
        highValueOrders.foreach((key, order) -> {
            log.info("High-value order processed: OrderId={}, Amount={}", 
                order.orderId(), order.totalAmount());
        });
        
        // Write regular orders
        regularOrders.to(
            "processed-orders",
            Produced.with(Serdes.String(), new JacksonJsonSerde<>(EnrichedOrderEvent.class))
        );
        
        regularOrders.foreach((key, order) -> {
            log.info("Regular order processed: OrderId={}, Amount={}", 
                order.orderId(), order.totalAmount());
        });
        
        return enrichedOrders;
    }
}

