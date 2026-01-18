package in.codefarm.notification_service.service;

import tools.jackson.databind.ObjectMapper;
import in.codefarm.notification_service.entity.IdempotencyKey;
import in.codefarm.notification_service.entity.Notification;
import in.codefarm.notification_service.event.CloudEventService;
import in.codefarm.notification_service.event.NotificationFailedEventData;
import in.codefarm.notification_service.event.NotificationSentEventData;
import in.codefarm.notification_service.event.ShippingLabelCreatedEventData;
import in.codefarm.notification_service.repository.IdempotencyKeyRepository;
import in.codefarm.notification_service.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Notification Service implementation with idempotency.
 * Sends email and SMS notifications for orders and publishes notification events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final CloudEventService cloudEventService;
    private final KafkaTemplate<String, io.cloudevents.CloudEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String NOTIFICATIONS_TOPIC = "notifications";
    
    // Mock customer email/phone mapping (in production, this would be from a database)
    private String getCustomerEmail(String customerId) {
        return "customer-" + customerId + "@example.com";
    }
    
    private String getCustomerPhone(String customerId) {
        return "+1-555-" + customerId.substring(customerId.length() - 4);
    }
    
    /**
     * Sends order placed notification.
     */
    public void sendOrderPlacedNotification(String orderId) {
        log.info("Sending order placed notification: orderId={}", orderId);
        // In production, this would send an email/SMS
    }
    
    /**
     * Sends payment processed notification.
     */
    public void sendPaymentProcessedNotification(String orderId) {
        log.info("Sending payment processed notification: orderId={}", orderId);
        // In production, this would send an email/SMS
    }
    
    /**
     * Sends payment failed notification.
     */
    public void sendPaymentFailedNotification(String orderId) {
        log.info("Sending payment failed notification: orderId={}", orderId);
        // In production, this would send an email/SMS
    }
    
    /**
     * Sends inventory reserved notification.
     */
    public void sendInventoryReservedNotification(String orderId) {
        log.info("Sending inventory reserved notification: orderId={}", orderId);
        // In production, this would send an email/SMS
    }
    
    /**
     * Sends inventory reservation failed notification.
     */
    public void sendInventoryReservationFailedNotification(String orderId) {
        log.info("Sending inventory reservation failed notification: orderId={}", orderId);
        // In production, this would send an email/SMS
    }
    
    /**
     * Sends order confirmation notification when shipping label is created.
     * Implements idempotency to ensure exactly-once processing.
     * 
     * @param eventId CloudEvents ID for idempotency check
     * @param orderId Order ID
     * @param customerId Customer ID
     * @param trackingNumber Tracking number
     * @param carrier Shipping carrier
     * @return Notification entity
     */
    @Transactional
    public Notification sendOrderConfirmation(String eventId, String orderId, String customerId, 
                                               String trackingNumber, String carrier) {
        log.info("Sending order confirmation: eventId={}, orderId={}, customerId={}", 
            eventId, orderId, customerId);
        
        // Idempotency check: if event already processed, return existing notification
        if (idempotencyKeyRepository.existsByEventId(eventId)) {
            log.info("Event already processed (idempotency): eventId={}, orderId={}", eventId, orderId);
            Notification existingNotification = notificationRepository.findByOrderId(orderId).stream()
                .filter(n -> n.getNotificationType() == Notification.NotificationType.ORDER_CONFIRMATION)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notification not found for order: " + orderId));
            
            log.info("Returning existing notification: notificationId={}, orderId={}", 
                existingNotification.getNotificationId(), orderId);
            return existingNotification;
        }
        
        // Generate notification ID
        String notificationId = "notification-" + UUID.randomUUID().toString();
        
        // Get customer contact info
        String email = getCustomerEmail(customerId);
        String phone = getCustomerPhone(customerId);
        
        // Create email notification
        String emailSubject = "Order Confirmation - Order #" + orderId;
        String emailBody = String.format(
            "Your order #%s has been confirmed and shipped!\n\n" +
            "Tracking Number: %s\n" +
            "Carrier: %s\n\n" +
            "Thank you for your purchase!",
            orderId, trackingNumber, carrier
        );
        
        Notification emailNotification = Notification.builder()
            .notificationId(notificationId + "-email")
            .orderId(orderId)
            .customerId(customerId)
            .notificationType(Notification.NotificationType.ORDER_CONFIRMATION)
            .channel(Notification.NotificationChannel.EMAIL)
            .recipient(email)
            .subject(emailSubject)
            .message(emailBody)
            .status(Notification.NotificationStatus.PENDING)
            .build();
        
        emailNotification = notificationRepository.save(emailNotification);
        log.info("Email notification record created: notificationId={}, orderId={}", 
            emailNotification.getNotificationId(), orderId);
        
        // Send email
        boolean emailSent = emailService.sendEmail(email, emailSubject, emailBody);
        
        if (emailSent) {
            emailNotification.setStatus(Notification.NotificationStatus.SENT);
            emailNotification = notificationRepository.save(emailNotification);
            log.info("Email sent successfully: notificationId={}, orderId={}", 
                emailNotification.getNotificationId(), orderId);
            
            // Publish NotificationSentEvent for email
            publishNotificationSentEvent(emailNotification);
        } else {
            emailNotification.setStatus(Notification.NotificationStatus.FAILED);
            emailNotification.setFailureReason("Email service unavailable");
            emailNotification = notificationRepository.save(emailNotification);
            log.warn("Email sending failed: notificationId={}, orderId={}", 
                emailNotification.getNotificationId(), orderId);
            
            // Publish NotificationFailedEvent for email
            publishNotificationFailedEvent(emailNotification, "Email service unavailable");
        }
        
        // Create SMS notification
        String smsMessage = String.format(
            "Order #%s confirmed! Tracking: %s. Carrier: %s",
            orderId, trackingNumber, carrier
        );
        
        Notification smsNotification = Notification.builder()
            .notificationId(notificationId + "-sms")
            .orderId(orderId)
            .customerId(customerId)
            .notificationType(Notification.NotificationType.ORDER_CONFIRMATION)
            .channel(Notification.NotificationChannel.SMS)
            .recipient(phone)
            .message(smsMessage)
            .status(Notification.NotificationStatus.PENDING)
            .build();
        
        smsNotification = notificationRepository.save(smsNotification);
        log.info("SMS notification record created: notificationId={}, orderId={}", 
            smsNotification.getNotificationId(), orderId);
        
        // Send SMS
        boolean smsSent = smsService.sendSms(phone, smsMessage);
        
        if (smsSent) {
            smsNotification.setStatus(Notification.NotificationStatus.SENT);
            smsNotification = notificationRepository.save(smsNotification);
            log.info("SMS sent successfully: notificationId={}, orderId={}", 
                smsNotification.getNotificationId(), orderId);
            
            // Publish NotificationSentEvent for SMS
            publishNotificationSentEvent(smsNotification);
        } else {
            smsNotification.setStatus(Notification.NotificationStatus.FAILED);
            smsNotification.setFailureReason("SMS service unavailable");
            smsNotification = notificationRepository.save(smsNotification);
            log.warn("SMS sending failed: notificationId={}, orderId={}", 
                smsNotification.getNotificationId(), orderId);
            
            // Publish NotificationFailedEvent for SMS
            publishNotificationFailedEvent(smsNotification, "SMS service unavailable");
        }
        
        // Save idempotency key (after both notifications processed)
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
            .eventId(eventId)
            .orderId(orderId)
            .eventType("com.ecommerce.shipping.label.created")
            .build();
        idempotencyKeyRepository.save(idempotencyKey);
        
        log.info("Order confirmation notifications processed: orderId={}, emailStatus={}, smsStatus={}", 
            orderId, emailNotification.getStatus(), smsNotification.getStatus());
        
        // Return email notification as primary
        return emailNotification;
    }
    
    /**
     * Publishes NotificationSentEvent to notifications topic.
     */
    private void publishNotificationSentEvent(Notification notification) {
        try {
            NotificationSentEventData eventData = NotificationSentEventData.builder()
                .notificationId(notification.getNotificationId())
                .orderId(notification.getOrderId())
                .customerId(notification.getCustomerId())
                .notificationType(notification.getNotificationType().name())
                .channel(notification.getChannel().name())
                .recipient(notification.getRecipient())
                .build();
            
            io.cloudevents.CloudEvent cloudEvent = cloudEventService.createCloudEvent(
                "com.ecommerce.notification.sent",
                notification.getOrderId(), // Subject: order ID
                eventData
            );
            
            // Use order ID as Kafka key for partition ordering
            kafkaTemplate.send(NOTIFICATIONS_TOPIC, notification.getOrderId(), cloudEvent);
            
            log.info("Published NotificationSentEvent: notificationId={}, orderId={}, eventId={}", 
                notification.getNotificationId(), notification.getOrderId(), cloudEvent.getId());
                
        } catch (Exception e) {
            log.error("Failed to publish NotificationSentEvent: notificationId={}, orderId={}", 
                notification.getNotificationId(), notification.getOrderId(), e);
            throw new RuntimeException("Failed to publish notification sent event", e);
        }
    }
    
    /**
     * Publishes NotificationFailedEvent to notifications topic.
     */
    private void publishNotificationFailedEvent(Notification notification, String failureReason) {
        try {
            NotificationFailedEventData eventData = NotificationFailedEventData.builder()
                .notificationId(notification.getNotificationId())
                .orderId(notification.getOrderId())
                .failureReason(failureReason)
                .build();
            
            io.cloudevents.CloudEvent cloudEvent = cloudEventService.createCloudEvent(
                "com.ecommerce.notification.failed",
                notification.getOrderId(), // Subject: order ID
                eventData
            );
            
            // Use order ID as Kafka key for partition ordering
            kafkaTemplate.send(NOTIFICATIONS_TOPIC, notification.getOrderId(), cloudEvent);
            
            log.info("Published NotificationFailedEvent: notificationId={}, orderId={}, eventId={}, reason={}", 
                notification.getNotificationId(), notification.getOrderId(), cloudEvent.getId(), failureReason);
                
        } catch (Exception e) {
            log.error("Failed to publish NotificationFailedEvent: notificationId={}, orderId={}", 
                notification.getNotificationId(), notification.getOrderId(), e);
            throw new RuntimeException("Failed to publish notification failed event", e);
        }
    }
}

