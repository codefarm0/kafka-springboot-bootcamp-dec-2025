package in.codefarm.notification.service.as.consumer.controller;

import in.codefarm.notification.service.as.consumer.entity.NotificationEntity;
import in.codefarm.notification.service.as.consumer.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    // Get all notifications
    @GetMapping
    public ResponseEntity<List<NotificationEntity>> getAllNotifications() {
        log.info("=== REST Endpoint: GET /api/notifications ===");
        
        var notifications = notificationService.findAll();
        log.info("Retrieved {} notifications", notifications.size());
        
        return ResponseEntity.ok(notifications);
    }
    
    // Get notification by order ID
    @GetMapping("/order/{orderId}")
    public ResponseEntity<NotificationEntity> getNotificationByOrderId(@PathVariable String orderId) {
        log.info("=== REST Endpoint: GET /api/notifications/order/{} ===", orderId);
        
        var notification = notificationService.findByOrderId(orderId);
        
        if (notification.isPresent()) {
            log.info("Found notification for order: {}", orderId);
            return ResponseEntity.ok(notification.get());
        } else {
            log.warn("No notification found for order: {}", orderId);
            return ResponseEntity.notFound().build();
        }
    }
    
    // Get notifications by customer ID
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<NotificationEntity>> getNotificationsByCustomerId(
        @PathVariable String customerId
    ) {
        log.info("=== REST Endpoint: GET /api/notifications/customer/{} ===", customerId);
        
        var notifications = notificationService.findByCustomerId(customerId);
        log.info("Retrieved {} notifications for customer: {}", notifications.size(), customerId);
        
        return ResponseEntity.ok(notifications);
    }
    
    // Get notifications by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<NotificationEntity>> getNotificationsByStatus(
        @PathVariable String status
    ) {
        log.info("=== REST Endpoint: GET /api/notifications/status/{} ===", status);
        
        var notifications = notificationService.findByStatus(status);
        log.info("Retrieved {} notifications with status: {}", notifications.size(), status);
        
        return ResponseEntity.ok(notifications);
    }
    
    // Get notifications by consumer method
    @GetMapping("/method/{consumerMethod}")
    public ResponseEntity<List<NotificationEntity>> getNotificationsByConsumerMethod(
        @PathVariable String consumerMethod
    ) {
        log.info("=== REST Endpoint: GET /api/notifications/method/{} ===", consumerMethod);
        
        var notifications = notificationService.findByConsumerMethod(consumerMethod);
        log.info("Retrieved {} notifications with method: {}", notifications.size(), consumerMethod);
        
        return ResponseEntity.ok(notifications);
    }
    
    // Get notification statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getNotificationStats() {
        log.info("=== REST Endpoint: GET /api/notifications/stats ===");
        
        var total = (long) notificationService.findAll().size();
        var sent = notificationService.countByStatus("SENT");
        var failed = notificationService.countByStatus("FAILED");
        var pending = notificationService.countByStatus("PENDING");
        
        var stats = Map.of(
            "total", total,
            "sent", sent,
            "failed", failed,
            "pending", pending
        );
        
        log.info("Notification stats - Total: {}, Sent: {}, Failed: {}, Pending: {}", 
            total, sent, failed, pending);
        
        return ResponseEntity.ok(stats);
    }
    
    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running!");
    }
}

