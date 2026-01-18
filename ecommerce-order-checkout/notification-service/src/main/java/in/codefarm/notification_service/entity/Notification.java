package in.codefarm.notification_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "notification_id", nullable = false, unique = true, length = 255)
    private String notificationId;
    
    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;
    
    @Column(name = "customer_id", nullable = false, length = 255)
    private String customerId;
    
    @Column(name = "notification_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;
    
    @Column(name = "channel", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;
    
    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;  // Email address or phone number
    
    @Column(name = "subject", length = 255)
    private String subject;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum NotificationType {
        ORDER_CONFIRMATION,
        ORDER_SHIPPED,
        ORDER_CANCELLED,
        PAYMENT_RECEIVED,
        SHIPPING_UPDATE
    }
    
    public enum NotificationChannel {
        EMAIL,
        SMS
    }
    
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }
}

