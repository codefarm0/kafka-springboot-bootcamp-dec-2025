package in.codefarm.notification.service.as.consumer.service;

import in.codefarm.notification.service.as.consumer.entity.NotificationEntity;
import in.codefarm.notification.service.as.consumer.event.OrderPlacedEvent;
import in.codefarm.notification.service.as.consumer.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationRepository notificationRepository;
    
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }
    
    @Transactional
    public NotificationEntity saveNotification(
        OrderPlacedEvent event,
        String notificationType,
        String status,
        String message,
        Integer partition,
        Long offset,
        String consumerGroup,
        String consumerMethod
    ) {
        log.debug("Saving notification for order {} to database", event.orderId());
        
        var notification = new NotificationEntity(
            event.orderId(),
            event.customerId(),
            notificationType,
            status,
            message,
            partition,
            offset,
            consumerGroup,
            consumerMethod
        );
        
        var saved = notificationRepository.save(notification);
        log.info("Notification saved to database - ID: {}, Order ID: {}", saved.getId(), event.orderId());
        
        return saved;
    }
    
    public void sendEmailNotification(OrderPlacedEvent event) {
        log.info("Sending email notification to customer: {}", event.customerId());
        log.info("Email: Your order {} for ${} has been placed successfully!", 
            event.orderId(), 
            event.totalAmount());
        
        // In real system: call email service
        // emailService.sendConfirmationEmail(event.customerId(), event.orderId(), event.totalAmount());
    }
    
    public void sendSMSNotification(OrderPlacedEvent event) {
        log.info("Sending SMS notification to customer: {}", event.customerId());
        log.info("SMS: Order {} confirmed. Amount: ${}", event.orderId(), event.totalAmount());
        
        // In real system: call SMS gateway
    }
    
    public void sendPushNotification(OrderPlacedEvent event) {
        log.info("Sending push notification to customer: {}", event.customerId());
        log.info("Push: New order {} placed!", event.orderId());
        
        // In real system: call push notification service
    }
    
    public java.util.Optional<NotificationEntity> findByOrderId(String orderId) {
        return notificationRepository.findByOrderId(orderId);
    }
    
    public List<NotificationEntity> findAll() {
        return notificationRepository.findAll();
    }
    
    public List<NotificationEntity> findByCustomerId(String customerId) {
        return notificationRepository.findByCustomerId(customerId);
    }
    
    public List<NotificationEntity> findByStatus(String status) {
        return notificationRepository.findByStatus(status);
    }
    
    public List<NotificationEntity> findByConsumerMethod(String consumerMethod) {
        return notificationRepository.findByConsumerMethod(consumerMethod);
    }
    
    public long countByStatus(String status) {
        return notificationRepository.countByStatus(status);
    }
}

