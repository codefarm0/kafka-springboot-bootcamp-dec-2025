package in.codefarm.streams.analytics.controller;

import in.codefarm.streams.analytics.dto.CustomerOrderCount;
import in.codefarm.streams.analytics.dto.CustomerTotalAmount;
import in.codefarm.streams.analytics.dto.WindowedOrderCount;
import in.codefarm.streams.analytics.processor.OrderAnalyticsProcessor;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class OrderAnalyticsController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderAnalyticsController.class);
    
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;
    
    public OrderAnalyticsController(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }
    
    /**
     * Get order count for a specific customer
     */
    @GetMapping("/count/{customerId}")
    public ResponseEntity<CustomerOrderCount> getOrderCount(@PathVariable String customerId) {
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams == null || !kafkaStreams.state().equals(KafkaStreams.State.RUNNING)) {
                return ResponseEntity.status(503).body(new CustomerOrderCount(customerId, 0L));
            }
            
            ReadOnlyKeyValueStore<String, Long> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    OrderAnalyticsProcessor.ORDER_COUNT_STORE,
                    QueryableStoreTypes.keyValueStore()
                )
            );
            
            Long count = store.get(customerId);
            return ResponseEntity.ok(new CustomerOrderCount(customerId, count != null ? count : 0L));
            
        } catch (Exception e) {
            log.error("Error getting order count for customer: {}", customerId, e);
            return ResponseEntity.status(500).body(new CustomerOrderCount(customerId, 0L));
        }
    }
    
    /**
     * Get total order amount for a specific customer
     */
    @GetMapping("/total/{customerId}")
    public ResponseEntity<CustomerTotalAmount> getTotalAmount(@PathVariable String customerId) {
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams == null || !kafkaStreams.state().equals(KafkaStreams.State.RUNNING)) {
                return ResponseEntity.status(503)
                    .body(new CustomerTotalAmount(customerId, BigDecimal.ZERO));
            }
            
            ReadOnlyKeyValueStore<String, BigDecimal> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    OrderAnalyticsProcessor.TOTAL_AMOUNT_STORE,
                    QueryableStoreTypes.keyValueStore()
                )
            );
            
            BigDecimal total = store.get(customerId);
            return ResponseEntity.ok(new CustomerTotalAmount(
                customerId,
                total != null ? total : BigDecimal.ZERO
            ));
            
        } catch (Exception e) {
            log.error("Error getting total amount for customer: {}", customerId, e);
            return ResponseEntity.status(500)
                .body(new CustomerTotalAmount(customerId, BigDecimal.ZERO));
        }
    }
    
    /**
     * Get windowed order counts for a specific customer within a time range
     */
    @GetMapping("/windowed/{customerId}")
    public ResponseEntity<List<WindowedOrderCount>> getWindowedCounts(
            @PathVariable String customerId,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) {
        
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams == null || !kafkaStreams.state().equals(KafkaStreams.State.RUNNING)) {
                return ResponseEntity.status(503).body(new ArrayList<>());
            }
            
            ReadOnlyWindowStore<String, Long> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    OrderAnalyticsProcessor.WINDOWED_COUNT_STORE,
                    QueryableStoreTypes.windowStore()
                )
            );
            
            Instant start = startTime != null 
                ? Instant.ofEpochMilli(startTime) 
                : Instant.now().minus(Duration.ofHours(1));
            Instant end = endTime != null 
                ? Instant.ofEpochMilli(endTime) 
                : Instant.now();
            
            List<WindowedOrderCount> results = new ArrayList<>();
            // Use fetchAll to get Windowed keys, then filter by customer and time range
            try (KeyValueIterator<Windowed<String>, Long> iterator = store.fetchAll(start, end)) {
                while (iterator.hasNext()) {
                    KeyValue<Windowed<String>, Long> next = iterator.next();
                    Windowed<String> windowedKey = next.key;
                    
                    // Filter by customer ID
                    if (windowedKey.key().equals(customerId)) {
                        results.add(new WindowedOrderCount(
                            customerId,
                            windowedKey.window().startTime(),
                            windowedKey.window().endTime(),
                            next.value
                        ));
                    }
                }
            }
            
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            log.error("Error getting windowed counts for customer: {}", customerId, e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    /**
     * Get all customer order counts
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getAllOrderCounts() {
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams == null || !kafkaStreams.state().equals(KafkaStreams.State.RUNNING)) {
                return ResponseEntity.status(503).body(new HashMap<>());
            }
            
            ReadOnlyKeyValueStore<String, Long> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    OrderAnalyticsProcessor.ORDER_COUNT_STORE,
                    QueryableStoreTypes.keyValueStore()
                )
            );
            
            Map<String, Long> counts = new HashMap<>();
            try (var iterator = store.all()) {
                iterator.forEachRemaining(kv -> counts.put(kv.key, kv.value));
            }
            
            return ResponseEntity.ok(counts);
            
        } catch (Exception e) {
            log.error("Error getting all order counts", e);
            return ResponseEntity.status(500).body(new HashMap<>());
        }
    }
    
    /**
     * Get all customer total amounts
     */
    @GetMapping("/totals")
    public ResponseEntity<Map<String, BigDecimal>> getAllTotalAmounts() {
        try {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            if (kafkaStreams == null || !kafkaStreams.state().equals(KafkaStreams.State.RUNNING)) {
                return ResponseEntity.status(503).body(new HashMap<>());
            }
            
            ReadOnlyKeyValueStore<String, BigDecimal> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(
                    OrderAnalyticsProcessor.TOTAL_AMOUNT_STORE,
                    QueryableStoreTypes.keyValueStore()
                )
            );
            
            Map<String, BigDecimal> totals = new HashMap<>();
            try (var iterator = store.all()) {
                iterator.forEachRemaining(kv -> totals.put(kv.key, kv.value));
            }
            
            return ResponseEntity.ok(totals);
            
        } catch (Exception e) {
            log.error("Error getting all total amounts", e);
            return ResponseEntity.status(500).body(new HashMap<>());
        }
    }
}

