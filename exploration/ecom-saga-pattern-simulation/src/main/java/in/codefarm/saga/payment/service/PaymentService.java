package in.codefarm.saga.payment.service;

import in.codefarm.saga.payment.entity.PaymentEntity;
import in.codefarm.saga.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PaymentService {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    
    private final PaymentRepository paymentRepository;
    
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    @Transactional
    public PaymentEntity processPayment(String paymentId, String orderId, String customerId,
                                       java.math.BigDecimal amount, String status,
                                       String transactionId) {
        var payment = new PaymentEntity(
            paymentId,
            orderId,
            customerId,
            amount,
            status,
            java.time.LocalDateTime.now(),
            transactionId
        );
        
        return paymentRepository.save(payment);
    }
    
    public Optional<PaymentEntity> findByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }
    
    public Optional<PaymentEntity> findByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
    
    @Transactional
    public void refundPayment(String orderId) {
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus("REFUNDED");
            paymentRepository.save(payment);
            log.info("Payment {} refunded for order {}", payment.getPaymentId(), orderId);
        });
    }
}

