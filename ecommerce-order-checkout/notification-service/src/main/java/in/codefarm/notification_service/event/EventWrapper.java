package in.codefarm.notification_service.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Simple event wrapper with required attributes.
 * Events are published as JSON strings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventWrapper<T> {
    private String source;           // e.g., "/notification-service"
    private String eventType;        // e.g., "com.ecommerce.notification.sent"
    private String eventId;          // Unique event ID (UUID)
    private OffsetDateTime eventTime; // Event timestamp
    private T data;                  // Event payload
}

