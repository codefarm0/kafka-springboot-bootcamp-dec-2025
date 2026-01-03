package in.codefarm.notification.service.as.consumer.controller;

import in.codefarm.notification.service.as.consumer.dto.ConsistencyCheckResponse;
import in.codefarm.notification.service.as.consumer.dto.InconsistencyResponse;
import in.codefarm.notification.service.as.consumer.entity.NotificationEntity;
import in.codefarm.notification.service.as.consumer.entity.PaymentEntity;
import in.codefarm.notification.service.as.consumer.service.NotificationService;
import in.codefarm.notification.service.as.consumer.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    
    public NotificationController(
        NotificationService notificationService,
        PaymentService paymentService
    ) {
        this.notificationService = notificationService;
        this.paymentService = paymentService;
    }
    
    @GetMapping
    public ResponseEntity<List<NotificationEntity>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.findAll());
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<NotificationEntity> getNotificationByOrderId(@PathVariable String orderId) {
        return notificationService.findByOrderId(orderId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<NotificationEntity>> getNotificationsByCustomerId(
        @PathVariable String customerId
    ) {
        return ResponseEntity.ok(notificationService.findByCustomerId(customerId));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<NotificationEntity>> getNotificationsByStatus(
        @PathVariable String status
    ) {
        return ResponseEntity.ok(notificationService.findByStatus(status));
    }
    
    @GetMapping("/method/{consumerMethod}")
    public ResponseEntity<List<NotificationEntity>> getNotificationsByConsumerMethod(
        @PathVariable String consumerMethod
    ) {
        return ResponseEntity.ok(notificationService.findByConsumerMethod(consumerMethod));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getNotificationStats() {
        var total = (long) notificationService.findAll().size();
        var sent = notificationService.countByStatus("SENT");
        var failed = notificationService.countByStatus("FAILED");
        var pending = notificationService.countByStatus("PENDING");
        
        return ResponseEntity.ok(Map.of(
            "total", total,
            "sent", sent,
            "failed", failed,
            "pending", pending
        ));
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running!");
    }
    
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentEntity>> getAllPayments() {
        return ResponseEntity.ok(paymentService.findAllPayments());
    }
    
    @GetMapping("/payments/order/{orderId}")
    public ResponseEntity<PaymentEntity> getPaymentByOrderId(@PathVariable String orderId) {
        return paymentService.findPaymentByOrderId(orderId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
//    @GetMapping("/inconsistencies")
//    public ResponseEntity<InconsistencyResponse> detectInconsistencies() {
//        var inconsistencies = notificationService.detectInconsistencies();
//        var details = inconsistencies.stream()
//            .map(inc -> new InconsistencyResponse.InconsistencyDetail(
//                (String) inc.get("type"),
//                inc.get("paymentId") != null ? (String) inc.get("paymentId") : null,
//                (String) inc.get("orderId"),
//                (String) inc.get("customerId"),
//                (String) inc.get("issue")
//            ))
//            .toList();
//
//        var message = inconsistencies.isEmpty()
//            ? "No inconsistencies found - all payments have corresponding orders"
//            : "Found " + inconsistencies.size() + " inconsistency(ies)";
//
//        return ResponseEntity.ok(new InconsistencyResponse(
//            inconsistencies.size(),
//            details,
//            message
//        ));
//    }
    
    @GetMapping("/consistency/order/{orderId}")
    public ResponseEntity<ConsistencyCheckResponse> checkOrderConsistency(@PathVariable String orderId) {
        var orderNotification = notificationService.findByOrderId(orderId);
        var payment = paymentService.findPaymentByOrderId(orderId);
        
        boolean isConsistent = paymentService.isConsistent(orderId);
        boolean hasOrder = orderNotification.isPresent();
        boolean hasPayment = payment.isPresent();
        
        String message = isConsistent 
            ? "Order and payment both exist - consistent state" 
            : (hasPayment && !hasOrder 
                ? "INCONSISTENCY: Payment exists but order does not (non-transactional issue!)"
                : (hasOrder && !hasPayment
                    ? "INCONSISTENCY: Order exists but payment does not"
                    : "Neither order nor payment found"));
        
        return ResponseEntity.ok(new ConsistencyCheckResponse(
            orderId,
            isConsistent,
            hasOrder,
            hasPayment,
            message
        ));
    }
}

