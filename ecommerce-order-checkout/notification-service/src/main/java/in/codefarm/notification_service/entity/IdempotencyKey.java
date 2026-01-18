package in.codefarm.notification_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Idempotency key entity to prevent duplicate event processing.
 */
@Entity
@Table(name = "idempotency_keys", 
       uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;  // CloudEvents ID
    
    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;
    
    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;
    
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;
    
    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }
}

