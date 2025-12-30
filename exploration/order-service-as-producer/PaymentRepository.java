package in.codefarm.notification.service.as.consumer.repository;

import in.codefarm.notification.service.as.consumer.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    
    Optional<PaymentEntity> findByPaymentId(String paymentId);
    
    Optional<PaymentEntity> findByOrderId(String orderId);
    
    List<PaymentEntity> findByCustomerId(String customerId);
    
    List<PaymentEntity> findByStatus(String status);
}

