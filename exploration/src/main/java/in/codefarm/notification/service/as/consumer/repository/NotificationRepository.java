package in.codefarm.notification.service.as.consumer.repository;

import in.codefarm.notification.service.as.consumer.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Optional<NotificationEntity> findByOrderId(String orderId);
    List<NotificationEntity> findByCustomerId(String customerId);
    List<NotificationEntity> findByStatus(String status);
    List<NotificationEntity> findByConsumerMethod(String consumerMethod);
    long countByStatus(String status);
}

