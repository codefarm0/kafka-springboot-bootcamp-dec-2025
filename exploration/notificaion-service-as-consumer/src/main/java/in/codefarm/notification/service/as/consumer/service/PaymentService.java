package in.codefarm.notification.service.as.consumer.service;

import in.codefarm.notification.service.as.consumer.entity.PaymentEntity;
import in.codefarm.notification.service.as.consumer.event.PaymentProcessedEvent;
import in.codefarm.notification.service.as.consumer.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    
    public PaymentService(
        PaymentRepository paymentRepository,
        NotificationService notificationService
    ) {
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
    }
    
    @Transactional
    public PaymentEntity savePayment(
        PaymentProcessedEvent event,
        Integer partition,
        Long offset,
        String consumerGroup
    ) {
        var payment = new PaymentEntity(
            event.paymentId(),
            event.orderId(),
            event.customerId(),
            event.amount(),
            event.status(),
            event.processedAt(),
            event.transactionId(),
            partition,
            offset,
            consumerGroup
        );
        
        return paymentRepository.save(payment);
    }
    
    public Optional<PaymentEntity> findPaymentByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }
    
    public Optional<PaymentEntity> findPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
    
    public List<PaymentEntity> findAllPayments() {
        return paymentRepository.findAll();
    }
    
    public List<PaymentEntity> findPaymentsByCustomerId(String customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }
    
    public List<PaymentEntity> findPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }
    
    /**
     * Check if payment has corresponding order
     * Returns true if both payment and order exist, false otherwise
     */
    public boolean hasCorrespondingOrder(String orderId) {
        return notificationService.findByOrderId(orderId).isPresent();
    }
    
    /**
     * Check consistency for a specific order
     * Returns true if both payment and order exist
     */
    public boolean isConsistent(String orderId) {
        var payment = findPaymentByOrderId(orderId);
        var order = notificationService.findByOrderId(orderId);
        return payment.isPresent() && order.isPresent();
    }
}

