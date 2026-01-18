package in.codefarm.order_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbox Event entity for transactional event publishing via Debezium CDC.
 * Events written to this table are automatically captured by Debezium
 * and published to Kafka as JSON strings.
 */
@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "aggregate_id", nullable = false, length = 255)
    private String aggregateId;  // Used as Kafka message key
    
    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;  // Event type (e.g., com.ecommerce.order.placed)
    
    @Column(name = "source", nullable = false, length = 255)
    @Builder.Default
    private String source = "/order-service";  // Event source
    
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;  // Event data as JSON string (includes source, eventType, eventId, eventTime, and data)
    
    @Column(name = "correlation_id", length = 255)
    private String correlationId;  // Optional: for distributed tracing
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

