package in.codefarm.notification_service.repository;

import in.codefarm.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Optional<Notification> findByNotificationId(String notificationId);
    
    List<Notification> findByOrderId(String orderId);
    
    List<Notification> findByCustomerId(String customerId);
    
    List<Notification> findByStatus(Notification.NotificationStatus status);
}

