package in.codefarm.order.service.as.producer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class KafkaTransactionConfig {
    
    @Bean
    public KafkaTransactionManager<String, Object> kafkaTransactionManager(
        ProducerFactory<String, Object> transactionalProducerFactory
    ) {
        return new KafkaTransactionManager<>(transactionalProducerFactory);
    }
}

