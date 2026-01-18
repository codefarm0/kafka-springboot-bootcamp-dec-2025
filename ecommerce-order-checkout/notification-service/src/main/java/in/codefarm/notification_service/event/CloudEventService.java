package in.codefarm.notification_service.event;

import tools.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for creating CloudEvents according to CNCF CloudEvents specification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudEventService {
    
    private static final String SOURCE_PREFIX = "/notification-service";
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a CloudEvent with the given type, subject, and data.
     */
    public CloudEvent createCloudEvent(String eventType, String subject, Object data) {
        try {
            byte[] dataBytes = objectMapper.writeValueAsBytes(data);
            
            return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType(eventType)
                .withSource(URI.create(SOURCE_PREFIX))
                .withSubject(subject)
                .withTime(OffsetDateTime.now())
                .withDataContentType("application/json")
                .withData(dataBytes)
                .build();
        } catch (Exception e) {
            log.error("Failed to create CloudEvent: type={}, subject={}", eventType, subject, e);
            throw new RuntimeException("Failed to create CloudEvent", e);
        }
    }
    
    /**
     * Creates a CloudEvent with correlation ID for distributed tracing.
     */
    public CloudEvent createCloudEventWithCorrelation(
            String eventType, 
            String subject, 
            String correlationId, 
            Object data) {
        try {
            byte[] dataBytes = objectMapper.writeValueAsBytes(data);
            
            return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType(eventType)
                .withSource(URI.create(SOURCE_PREFIX))
                .withSubject(subject)
                .withTime(OffsetDateTime.now())
                .withDataContentType("application/json")
                .withExtension("correlationid", correlationId)
                .withData(dataBytes)
                .build();
        } catch (Exception e) {
            log.error("Failed to create CloudEvent with correlation: type={}, subject={}, correlationId={}", 
                eventType, subject, correlationId, e);
            throw new RuntimeException("Failed to create CloudEvent with correlation", e);
        }
    }
}

