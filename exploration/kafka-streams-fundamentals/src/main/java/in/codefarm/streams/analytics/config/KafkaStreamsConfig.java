package in.codefarm.streams.analytics.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Kafka Streams analytics.
 * KafkaStreams and StreamsBuilderFactoryBean are auto-configured by Spring Boot
 * when @EnableKafkaStreams is present on the main application class.
 */
@Configuration
public class KafkaStreamsConfig {
    // KafkaStreams and StreamsBuilderFactoryBean are auto-configured
    // Access via StreamsBuilderFactoryBean in controllers/services
}

