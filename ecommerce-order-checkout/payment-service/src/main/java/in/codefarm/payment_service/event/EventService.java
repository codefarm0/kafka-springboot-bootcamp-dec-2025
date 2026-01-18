package in.codefarm.payment_service.event;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for creating event JSON strings with required attributes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    
    private static final String SOURCE_PREFIX = "/payment-service";
    private final ObjectMapper objectMapper;
    
    /**
     * Creates an event JSON string with the given type, subject, and data.
     * 
     * @param eventType Event type (e.g., "com.ecommerce.payment.processed")
     * @param eventId Optional event ID (if null, generates a new UUID)
     * @param data Event data payload
     * @return JSON string representation of the event
     */
    public <T> String createEvent(String eventType, String eventId, T data) {
        try {
            EventWrapper<T> event = EventWrapper.<T>builder()
                .source(SOURCE_PREFIX)
                .eventType(eventType)
                .eventId(eventId != null ? eventId : UUID.randomUUID().toString())
                .eventTime(OffsetDateTime.now())
                .data(data)
                .build();
            
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to create event: type={}", eventType, e);
            throw new RuntimeException("Failed to create event", e);
        }
    }
    
    /**
     * Creates an event JSON string with auto-generated event ID.
     */
    public <T> String createEvent(String eventType, T data) {
        return createEvent(eventType, null, data);
    }
}

