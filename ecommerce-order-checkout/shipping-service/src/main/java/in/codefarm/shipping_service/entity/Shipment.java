package in.codefarm.shipping_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "shipment_id", nullable = false, unique = true, length = 255)
    private String shipmentId;
    
    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;
    
    @Column(name = "customer_id", nullable = false, length = 255)
    private String customerId;
    
    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;
    
    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;
    
    @Column(name = "carrier", length = 100)
    private String carrier;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;
    
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
    
    public enum ShipmentStatus {
        PENDING,
        LABEL_CREATED,
        IN_TRANSIT,
        DELIVERED,
        FAILED
    }
}

