package in.codefarm.saga.config;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.SerializationFeature;
import in.codefarm.saga.event.EventWrapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();
    }
    
    private Map<String, Object> baseProducerConfigs() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        return configProps;
    }
    
    @Bean
    public ProducerFactory<String, EventWrapper<?>> eventWrapperProducerFactory(JsonMapper jsonMapper) {
        Map<String, Object> configProps = baseProducerConfigs();
        
        // Create serializer with JsonMapper in constructor (Spring Boot 4 expects JsonMapper)
        JacksonJsonSerializer<EventWrapper<?>> serializer = new JacksonJsonSerializer<>(jsonMapper);
        
        // Set the serializer class in config
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        
        // Create factory with config and serializer
        return new DefaultKafkaProducerFactory<>(configProps, new StringSerializer(), serializer);
    }
    
    @Bean
    public KafkaTemplate<String, EventWrapper<?>> eventWrapperKafkaTemplate(
        ProducerFactory<String, EventWrapper<?>> eventWrapperProducerFactory
    ) {
        return new KafkaTemplate<>(eventWrapperProducerFactory);
    }
}

