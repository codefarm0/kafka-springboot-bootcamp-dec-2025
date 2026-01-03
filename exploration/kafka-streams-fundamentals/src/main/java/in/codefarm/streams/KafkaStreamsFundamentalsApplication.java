package in.codefarm.streams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class KafkaStreamsFundamentalsApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaStreamsFundamentalsApplication.class, args);
	}

}

