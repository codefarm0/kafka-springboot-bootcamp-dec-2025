package in.codefarm.streams.analytics.processor;

import in.codefarm.streams.event.OrderPlacedEvent;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.math.BigDecimal;
import java.time.Duration;

@Configuration
public class OrderAnalyticsProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(OrderAnalyticsProcessor.class);
    
    // State store names for materialized views
    public static final String ORDER_COUNT_STORE = "order-count-store";
    public static final String TOTAL_AMOUNT_STORE = "total-amount-store";
    public static final String WINDOWED_COUNT_STORE = "windowed-count-store";
    
    @Bean
    public KStream<String, OrderPlacedEvent> processOrderAnalytics(StreamsBuilder streamsBuilder) {
        // Read from orders topic
        KStream<String, OrderPlacedEvent> orders = streamsBuilder.stream(
            "orders",
            Consumed.with(Serdes.String(), new JacksonJsonSerde<>(OrderPlacedEvent.class))
        );
        
        log.info("Order analytics processor initialized");
        
        // 1. Count orders per customer (stateful aggregation)
        KTable<String, Long> orderCounts = orders
            .groupBy((key, order) -> order.customerId())
            .count(
                Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as(ORDER_COUNT_STORE)
                    .withKeySerde(Serdes.String())
                    .withValueSerde(Serdes.Long())
            );
        
        // Log count updates
        orderCounts.toStream().foreach((customerId, count) -> {
            log.info("Order count updated - CustomerId: {}, Count: {}", customerId, count);
        });
        
        // 2. Sum total order amounts per customer (stateful aggregation)
        KTable<String, BigDecimal> totalAmounts = orders
            .groupBy((key, order) -> order.customerId())
            .aggregate(
                () -> BigDecimal.ZERO,  // Initial value
                (customerId, order, total) -> total.add(order.totalAmount()),  // Aggregator
                Materialized.<String, BigDecimal, KeyValueStore<Bytes, byte[]>>as(TOTAL_AMOUNT_STORE)
                    .withKeySerde(Serdes.String())
                    .withValueSerde(new JacksonJsonSerde<>(BigDecimal.class))
            );
        
        // Log total amount updates
        totalAmounts.toStream().foreach((customerId, total) -> {
            log.info("Total amount updated - CustomerId: {}, Total: {}", customerId, total);
        });
        
        // 3. Windowed count: Count orders per minute (windowed aggregation)
        KTable<Windowed<String>, Long> windowedCounts = orders
            .groupBy((key, order) -> order.customerId())
            .windowedBy(
                TimeWindows.ofSizeAndGrace(
                    Duration.ofMinutes(1),  // Window size: 1 minute
                    Duration.ofSeconds(10)  // Grace period: 10 seconds
                )
            )
            .count(
                Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(WINDOWED_COUNT_STORE)
                    .withKeySerde(Serdes.String())
                    .withValueSerde(Serdes.Long())
            );
        
        // Log windowed count updates
        windowedCounts.toStream().foreach((windowedKey, count) -> {
            log.info("Windowed count updated - CustomerId: {}, Window: {} - {}, Count: {}", 
                windowedKey.key(),
                windowedKey.window().startTime(),
                windowedKey.window().endTime(),
                count);
        });
        
        return orders;
    }
}

