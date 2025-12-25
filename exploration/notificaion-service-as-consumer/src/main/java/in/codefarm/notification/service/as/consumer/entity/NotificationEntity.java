package in.codefarm.notification.service.as.consumer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String orderId;
    private String customerId;
    private String notificationType;  // "EMAIL", "SMS", "PUSH", etc.
    private String status;  // "SENT", "FAILED", "PENDING"
    private String message;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    
    // Consumer metadata
    @Column(name = "kafka_partition")
    private Integer partition;
    @Column(name = "msg_offset")
    private Long offset;
    private String consumerGroup;
    private String consumerMethod;  // Which consumer method processed this
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Default constructor for JPA
    public NotificationEntity() {
    }
    
    // Constructor
    public NotificationEntity(String orderId, String customerId, String notificationType, 
                              String status, String message, Integer partition, Long offset,
                              String consumerGroup, String consumerMethod) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.notificationType = notificationType;
        this.status = status;
        this.message = message;
        this.partition = partition;
        this.offset = offset;
        this.consumerGroup = consumerGroup;
        this.consumerMethod = consumerMethod;
        this.sentAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Integer getPartition() {
        return partition;
    }
    
    public void setPartition(Integer partition) {
        this.partition = partition;
    }
    
    public Long getOffset() {
        return offset;
    }
    
    public void setOffset(Long offset) {
        this.offset = offset;
    }
    
    public String getConsumerGroup() {
        return consumerGroup;
    }
    
    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }
    
    public String getConsumerMethod() {
        return consumerMethod;
    }
    
    public void setConsumerMethod(String consumerMethod) {
        this.consumerMethod = consumerMethod;
    }
}

