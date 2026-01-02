package in.codefarm.saga.config;

import tools.jackson.databind.json.JsonMapper;
import in.codefarm.saga.event.EventWrapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean("consumerJsonMapper")
    public JsonMapper consumerJsonMapper() {
        return JsonMapper.builder()
            .build();
    }
    
    private Map<String, Object> baseConsumerConfigs() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        return configProps;
    }
    
    @Bean
    public ConsumerFactory<String, EventWrapper<?>> eventWrapperConsumerFactory(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
        JsonMapper consumerJsonMapper
    ) {
        Map<String, Object> configProps = baseConsumerConfigs();
        
        // Create deserializer with JsonMapper in constructor (Spring Boot 4 expects JsonMapper)
        JacksonJsonDeserializer<EventWrapper<?>> deserializer = 
            new JacksonJsonDeserializer<>(EventWrapper.class, consumerJsonMapper);
        deserializer.addTrustedPackages("*");
        
        // Set the deserializer class in config
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        
        // Create factory with config and deserializer
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), deserializer);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventWrapper<?>> eventWrapperKafkaListenerContainerFactory(
        ConsumerFactory<String, EventWrapper<?>> eventWrapperConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, EventWrapper<?>> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventWrapperConsumerFactory);
        return factory;
    }
}

