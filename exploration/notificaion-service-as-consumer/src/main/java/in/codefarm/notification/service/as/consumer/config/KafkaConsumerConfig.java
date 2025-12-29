package in.codefarm.notification.service.as.consumer.config;

import in.codefarm.notification.service.as.consumer.event.OrderPlacedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Base consumer factory configuration
    private Map<String, Object> baseConsumerConfigs() {
        Map<String, Object> configProps = new HashMap<>();

        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);

        // JSON deserializer configuration
        configProps.put("spring.json.trusted.packages", "*");
        configProps.put("spring.json.use.type.headers", false);
        configProps.put("spring.json.value.default.type","in.codefarm.notification.service.as.consumer.event.OrderPlacedEvent");
        // Auto offset reset
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Session timeout
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);

        // Heartbeat interval
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        // Max poll records
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

        // Max poll interval
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

        configProps.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, List.of(CooperativeStickyAssignor.class)); //todo

        return configProps;
    }

    // Consumer factory for auto-commit (default)
    @Bean
    public ConsumerFactory<String, OrderPlacedEvent> autoCommitConsumerFactory() {
        Map<String, Object> configProps = baseConsumerConfigs();

        // Auto commit configuration
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    // Consumer factory for manual commit
    @Bean
    public ConsumerFactory<String, OrderPlacedEvent> manualCommitConsumerFactory() {
        Map<String, Object> configProps = baseConsumerConfigs();

        // Manual commit configuration
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-manual-group");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    // Consumer factory for batch consumption
    @Bean
    public ConsumerFactory<String, OrderPlacedEvent> batchConsumerFactory() {
        Map<String, Object> configProps = baseConsumerConfigs();

        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-batch-group");
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // Larger batch

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    // Container factory for auto-commit
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderPlacedEvent> autoCommitConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(autoCommitConsumerFactory);
        factory.setConcurrency(3); // 3 consumer threads
        return factory;
    }

    // Container factory for manual commit
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> manualCommitKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderPlacedEvent> manualCommitConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(manualCommitConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        return factory;
    }

    // Container factory for batch consumption
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> batchKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderPlacedEvent> batchConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(batchConsumerFactory);
        factory.setBatchListener(true); // Enable batch mode
        factory.setConcurrency(3);
        return factory;
    }
}

