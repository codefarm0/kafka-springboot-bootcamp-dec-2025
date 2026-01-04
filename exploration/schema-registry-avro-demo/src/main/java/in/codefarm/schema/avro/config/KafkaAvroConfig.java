package in.codefarm.schema.avro.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import in.codefarm.schema.avro.event.OrderPlacedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Avro Configuration
 * 
 * This configuration sets up Kafka producers and consumers with Avro serialization
 * and Schema Registry integration.
 * 
 * DEMO NOTES:
 * - Uses KafkaAvroSerializer/Deserializer for Avro serialization
 * - Connects to Schema Registry for schema management
 * - Automatically registers schemas on first use
 * - Supports schema evolution through compatibility rules
 */
@Configuration
public class KafkaAvroConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.producer.properties.schema.registry.url}")
    private String schemaRegistryUrl;
    
    /**
     * Producer Factory for Avro messages
     * 
     * Configuration:
     * - Uses KafkaAvroSerializer for value serialization
     * - Connects to Schema Registry
     * - Automatically registers schema on first send
     */
    @Bean
    public ProducerFactory<String, OrderPlacedEvent> avroProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        configProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    /**
     * KafkaTemplate for sending Avro messages
     */
    @Bean
    public KafkaTemplate<String, OrderPlacedEvent> avroKafkaTemplate(
            @Qualifier("avroProducerFactory")ProducerFactory<String, OrderPlacedEvent> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }


    /**
     * Consumer Factory for Avro messages
     * 
     * Configuration:
     * - Uses KafkaAvroDeserializer for value deserialization
     * - Connects to Schema Registry to fetch schemas
     * - Uses specific Avro reader for type-safe deserialization
     */
    @Bean
    public ConsumerFactory<String, OrderPlacedEvent> avroConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "order-avro-consumer-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        configProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        // Use specific Avro reader for type-safe deserialization (returns generated Avro classes)
        configProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    /**
     * Kafka Listener Container Factory for @KafkaListener
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> avroKafkaListenerContainerFactory(
        ConsumerFactory<String, OrderPlacedEvent> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
}

